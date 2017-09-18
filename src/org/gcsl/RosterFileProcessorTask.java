package org.gcsl;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import javafx.concurrent.Task;
import org.gcsl.db.AthleteDbo;
import org.gcsl.db.TeamDbo;
import org.gcsl.model.Athlete;
import org.gcsl.model.ProcessArchiveItem;
import org.gcsl.model.Team;
import org.gcsl.sdif.SdifException;
import org.gcsl.sdif.SdifFileDescription;
import org.gcsl.sdif.SdifReader;
import org.gcsl.sdif.SdifRec;
import org.gcsl.util.Utils;


public class RosterFileProcessorTask extends Task<String>
{
    private List<ProcessArchiveItem> archiveItems = Collections.emptyList();
    private Connection dbConn;

    public RosterFileProcessorTask(Connection conn, List<ProcessArchiveItem> archiveFileItems)
    {
        dbConn = conn;
        archiveItems = archiveFileItems;
    }

    @Override protected String call()
    {
        int curItem  = 0;
        int numItems = archiveItems.size();
        System.out.printf("Inside RosterFileProcessorTask::call(). archiveItems.size()=%d %n", archiveItems.size());

        for (ProcessArchiveItem archiveItem : archiveItems)
        {
            if (isCancelled())  { break; }
            curItem++;
            try {
                dbConn.setAutoCommit(false); // use transactions for performance reasons.

                String archiveFilePath = archiveItem.getDirectory() + File.separator + archiveItem.getName();
                Team csvTeam = readRosterArchive(archiveFilePath);
                Team dbTeam  = TeamDbo.find(dbConn, csvTeam);
                if (dbTeam == null) {
                    // Team not in DB, add it to DB.
                    dbTeam = TeamDbo.insert(dbConn, csvTeam);
                }
                else {
                    // Team in DB, get all the athletes associated with this team.
                    dbTeam.addRoster(TeamDbo.retrieveAthletes(dbConn, dbTeam));
                }

                csvTeam.setId(dbTeam.getId());   // now we know the team ID, assign it to the csvTeam

                if (dbTeam.getAthletes().size() == 0) {
                    insertAthletes(csvTeam.getAthletes());
                }
                else {
                    mergeAthletes(dbTeam, csvTeam);
                }
                dbConn.commit();
            }
            catch (SdifException | IOException | SQLException e) {
                try { dbConn.rollback(); } catch (SQLException e1) { ; }
                updateMessage("Task Failed ..." + e.getMessage());
                return "Failure";
            }
            finally {
                try { dbConn.setAutoCommit(true); } catch (SQLException e) { ; }
            }

            updateMessage("Processing archive: " + archiveItem.getName());
            updateProgress(curItem, numItems);
        }
        updateMessage("Roster files processed successfully.");
        return "Success";
    }


    // Remove a set of athletes from the DB.
    // Throw an SQLException if there is a DB error.
    private void deleteAthletes(Set<Athlete> athletes) throws SQLException
    {
        dbConn.setAutoCommit(false);
        for (Athlete athlete : athletes) {
            System.out.printf("Deleting athlete %s %n", athlete.toString());
            AthleteDbo.remove(dbConn, athlete);
        }
    }

    // Extract the CL2 or HY3 file from the archive file.  Return the path to the extracted file.
    // Throw an SdifException if the archive does not contain a CL2 or HY3 file.
    private String extractRosterFromArchive(String archivePath) throws SdifException, IOException
    {
        List<String> archiveContents = Utils.getFileNamesFromArchive(archivePath);

        // look for CL2 file first, then HY3
        for (String fileName : archiveContents) {
            if (fileName.toLowerCase().endsWith(".cl2")) {
                return Utils.getFileFromArchive(archivePath, fileName);
            }
        }

        for (String fileName : archiveContents) {
            if (fileName.toLowerCase().endsWith(".hy3")) {
                return Utils.getFileFromArchive(archivePath, fileName);
            }
        }

        throw new SdifException("Roster archive does not contain a .cl2 or hy3 file.");
    }

    // Insert a set of athletes into the DB.
    // Throws an SQLException if there is a DB error.
    private void insertAthletes(Set<Athlete> athletes) throws SQLException
    {
        System.out.printf("Inserting %d athletes into the DB. %n", athletes.size());
        for (Athlete a : athletes) {
            AthleteDbo.insert(dbConn, a);
        }
    }

    private void mergeAthletes(Team existingTeam, Team newTeam) throws SQLException
    {
        Set<Athlete> addMeCache = new HashSet<>();
        // Copy the existing athletes to initialize the deleteMeCache.  We'll eventually delete
        // all athletes from the DB that remain in this set.
        Set<Athlete> deleteMeCache = new HashSet<>(existingTeam.getAthletes());

        for (Athlete athlete : newTeam.getAthletes()) {
            // if athlete is removed from the delete cache, that means the athlete
            // was on the existing roster and we don't need to do anything.
            // Otherwise, the athlete was not on the existing roster and needs
            // to put them the add cache.
            if (! deleteMeCache.remove(athlete)) {
                addMeCache.add(athlete);
            }
        }

        System.out.printf("mergeAthletes:  New roster for team %s, delete count=%d, add count=%d %n",
                          newTeam.getName(), deleteMeCache.size(), addMeCache.size());
        if (deleteMeCache.size() > 0)  { deleteAthletes(deleteMeCache); }
        if (addMeCache.size() > 0)     { insertAthletes(addMeCache); }
    }


    private Optional<Team> processRosterRecs(List<SdifRec> rosterRecs) throws SdifException
    {
        Optional<Team> optTeam = Optional.empty();
        Team team = null;

        for (SdifRec rec : rosterRecs) {
            switch (rec.getType()) {
                case TEAM_ID_REC: {
                    if (optTeam.isPresent()) {
                        throw new SdifException("More than 1 team defined in the roster file");
                    }
                    team = Team.fromSdifData(rec);
                    optTeam = Optional.of(team);
                    break;
                }

                case INDIVIDUAL_ADMIN_REC:  // fall through
                case INDIVIDUAL_EVENT_REC: {

                    Athlete athlete = Athlete.fromSdif(rec);
                    optTeam.orElseThrow( () -> new SdifException("Athlete rec before team rec.") )
                           .addAthlete(athlete);
                    break;
                }

                default:
                    break;  // don't care about others
            }
        }

        return optTeam;
    }

    // Read a Roster Archive file and return a Team object representing the team roster.
    // Throw an IOException if there are errors reading from the roster archive file.
    // Throw an SdifException if the roster file contents are not valid.
    private Team readRosterArchive(String archiveFile) throws SdifException, IOException
    {
        String  rosterFilePath;
        boolean rosterFileIsTemp = false;
        SdifFileDescription.SdifFileType rosterFileType = SdifFileDescription.SdifFileType.UNKNOWN;
        System.out.println("processing archive file: " + archiveFile);

        // Check for ZIP archive
        Utils.ARCHIVE_FILE_TYPE fileType = Utils.getArchiveFileType(archiveFile);
        switch (fileType) {
            case ZIP : rosterFilePath = extractRosterFromArchive(archiveFile);
                rosterFileIsTemp = true;
                rosterFileType = SdifFileDescription.SdifFileType.VENDOR_DEFINED;
                break;
            case SD3:  rosterFilePath = archiveFile;
                rosterFileType = SdifFileDescription.SdifFileType.MEET_REGISTRATION;
                break;
            default:   throw new SdifException("Unknown roster file archive.  Filepath=" + archiveFile);
        }

        Team team = readRosterFile(rosterFilePath, rosterFileType);

        if (rosterFileIsTemp) {
            File f = new File(rosterFilePath);
            f.delete();
        }

        return team;
    }

    // Read a roster file (CL2, HY3 or SD3) and create a Team object from the roster file data.
    // Throw an SdifException if the roster file is not valid.
    private Team readRosterFile(String rosterFilePath, SdifFileDescription.SdifFileType fileTypeExpected) throws SdifException
    {
        SdifReader sdifReader = new SdifReader(rosterFilePath);
        SdifFileDescription.SdifFileType fileType = sdifReader.getFileDescription().getFileType();

        if (fileType != fileTypeExpected) {
            throw new SdifException("File type mismatch, expected " + fileTypeExpected + ", found " + fileType);
        }

        List<SdifRec>  recs = sdifReader.readFile();
        Optional<Team> team = processRosterRecs(recs);

        return team.orElseThrow( () -> new SdifException("No team defined in the SDIF file.") );
    }
}
