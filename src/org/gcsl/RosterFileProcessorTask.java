package org.gcsl;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
                String archiveFilePath = archiveItem.getDirectory() + File.separator + archiveItem.getName();
                Team csvTeam = readRosterArchive(archiveFilePath);
                Team dbTeam  = TeamDbo.find(dbConn, csvTeam);
                if (dbTeam == null) {
                    dbTeam = TeamDbo.insert(dbConn, csvTeam);
                }

                // now we know the team ID, assign it to the csvTeam
                csvTeam.setId(dbTeam.getId());

                // If the team read from the DB does not have any athletes, add all the
                // csvTeam athletes to the DB.  Otherwise, merge the csvTeam athletes
                // with the dbTeam athletes.  Merge may add and/or delete athletes.
                if (dbTeam.getAthletes().size() == 0) {
                    for (Athlete a : csvTeam.getAthletes()) {
                        AthleteDbo.insert(dbConn, a);
                    }
                }
                else {
                    // merge athletes (dbTeam, csvTeam
                }

            }
            catch (SdifException | IOException | SQLException e) {
                updateMessage("Task Failed ..." + e.getMessage());
                return "Failure";
            }

            updateMessage("Processing archive: " + archiveItem.getName());
            updateProgress(curItem, numItems);
        }
        updateMessage("Roster files processed successfully.");
        return "Success";
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
