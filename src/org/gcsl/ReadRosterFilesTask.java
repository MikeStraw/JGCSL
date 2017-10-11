package org.gcsl;

import javafx.concurrent.Task;
import org.gcsl.model.Athlete;
import org.gcsl.model.ProcessArchiveItem;
import org.gcsl.model.Team;
import org.gcsl.sdif.SdifException;
import org.gcsl.sdif.SdifFileDescription;
import org.gcsl.sdif.SdifReader;
import org.gcsl.sdif.SdifRec;
import org.gcsl.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

// This class will read the roster files contained within the list of archive files and
// return a list of Team objects representing those roster files.
public class ReadRosterFilesTask extends Task<List<Team>>
{
    private List<ProcessArchiveItem> archiveItems = Collections.emptyList();

    ReadRosterFilesTask(List<ProcessArchiveItem> archiveFileItems)
    {
        archiveItems = archiveFileItems;
    }

    @Override
    protected List<Team> call() throws Exception
    {
        int curItem  = 0;
        int numItems = archiveItems.size();
        List<Team> teams = new Vector<>();
        System.out.printf("Inside ReadRosterFilesTask::call(). archiveItems.size()=%d %n", archiveItems.size());

        for (ProcessArchiveItem archiveItem : archiveItems)
        {
            if (isCancelled())  { break; }
            curItem++;

            Team sdifTeam = readRosterArchive(archiveItem);
            teams.add(sdifTeam);

            updateMessage("Processing archive: " + archiveItem.getName());
            updateProgress(curItem, numItems);
        }
        updateMessage("Roster files read successfully.");

        return teams;
    }


    // Extracts the roster file from the archive.  Return the path to the extracted archive file.
    private String extractRosterArchiveFile(ProcessArchiveItem archiveItem) throws IOException, SdifException
    {
        String []archiveContents = orderRosterFiles(archiveItem.getContents());
        String   archiveFilePath = archiveItem.getDirectory() + File.separator + archiveItem.getName();
        String   rosterFilePath;

        // Extract the results file from the archive
        Utils.ARCHIVE_FILE_TYPE fileType = Utils.getArchiveFileType(archiveFilePath);
        switch (fileType) {
            case ZIP : rosterFilePath = Utils.getFileFromArchive(archiveFilePath, archiveContents[0]);
                break;
            case SD3:  rosterFilePath = archiveFilePath;
                break;
            default:   throw new SdifException("Unknown roster file archive.  Filepath=" + archiveFilePath);
        }

        return rosterFilePath;
    }


    // Order the archive content based on whether to prioritize .CL2 files over .HY3 files.
    private String [] orderRosterFiles(String archiveContents)
    {
        // TODO: really should check a config entry, for now just assume that we want .CL2 entries before .HY3
        return archiveContents.split(", ");
    }


    private Optional<Team> processRosterRecs(List<SdifRec> rosterRecs) throws SdifException
    {
        Optional<Team> optTeam = Optional.empty();
        Team team;

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
    private Team readRosterArchive(ProcessArchiveItem archiveItem) throws SdifException, IOException
    {
        String  archiveFilePath  = archiveItem.getDirectory() + File.separator + archiveItem.getName();
        String  rosterFilePath   = extractRosterArchiveFile(archiveItem);
        boolean rosterFileIsTemp = ! archiveFilePath.equals(rosterFilePath);  // not equal means is a temp file

        Utils.ARCHIVE_FILE_TYPE archiveFileType  = Utils.getArchiveFileType(archiveFilePath);
        SdifFileDescription.SdifFileType rosterFileTypeExpected;
        switch (archiveFileType) {
            case ZIP : rosterFileTypeExpected = SdifFileDescription.SdifFileType.VENDOR_DEFINED;     break;
            case SD3:  rosterFileTypeExpected = SdifFileDescription.SdifFileType.MEET_REGISTRATION;  break;
            default:   throw new SdifException("Unknown roster file archive.  Filepath=" + archiveFilePath);
        }

        Team team = readRosterFile(rosterFilePath, rosterFileTypeExpected);
        if (rosterFileIsTemp) {
            Files.deleteIfExists(Paths.get(rosterFilePath));
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
