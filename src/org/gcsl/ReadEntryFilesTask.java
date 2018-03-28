package org.gcsl;

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
import java.util.List;
import java.util.Optional;

public class ReadEntryFilesTask extends ReadSdifArchiveTask<Team>
{

    ReadEntryFilesTask(List<ProcessArchiveItem> archiveFileItems)
    {
        super(archiveFileItems);
    }

    // Read an Entry Archive file and return a Team object representing the team roster.
    // Throw an IOException if there are errors reading from the entry archive file.
    // Throw an SdifException if the entry file contents are not valid.
    @Override
    Team processArchiveItem(ProcessArchiveItem archiveItem) throws SdifException, IOException
    {
        String  archiveFilePath  = archiveItem.getDirectory() + File.separator + archiveItem.getName();
        String  rosterFilePath   = extractSdifFileFromArchive(archiveItem);
        boolean rosterFileIsTemp = ! archiveFilePath.equals(rosterFilePath);  // not equal means is a temp file

        Utils.ARCHIVE_FILE_TYPE archiveFileType  = Utils.getArchiveFileType(archiveFilePath);
        SdifFileDescription.SdifFileType rosterFileTypeExpected;
        switch (archiveFileType) {
            case ZIP : rosterFileTypeExpected = SdifFileDescription.SdifFileType.VENDOR_DEFINED;     break;
            case SD3:  rosterFileTypeExpected = SdifFileDescription.SdifFileType.MEET_REGISTRATION;  break;
            default:   throw new SdifException("Unknown roster file archive.  Filepath=" + archiveFilePath);
        }

        Team team = readEntryFile(rosterFilePath, rosterFileTypeExpected);
        if (rosterFileIsTemp) {
            Files.deleteIfExists(Paths.get(rosterFilePath));
        }

        return team;
    }


    private Optional<Team> processEntryRecs(List<SdifRec> rosterRecs) throws SdifException
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
                case INDIVIDUAL_EVENT_REC:  // fall through
                case RELAY_NAME_REC:  {
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

    // Read a entry file (CL2, HY3 or SD3) and create a Team object from the entry file data.
    // Throw an SdifException if the roster file is not valid.
    private Team readEntryFile(String rosterFilePath, SdifFileDescription.SdifFileType fileTypeExpected) throws SdifException
    {
        SdifReader sdifReader = new SdifReader(rosterFilePath);
        SdifFileDescription.SdifFileType fileType = sdifReader.getFileDescription().getFileType();

        if (fileType != fileTypeExpected) {
            throw new SdifException("File type mismatch, expected " + fileTypeExpected + ", found " + fileType);
        }

        List<SdifRec>  recs = sdifReader.readFile();
        Optional<Team> team = processEntryRecs(recs);

        return team.orElseThrow( () -> new SdifException("No team defined in the SDIF file.") );
    }
}
