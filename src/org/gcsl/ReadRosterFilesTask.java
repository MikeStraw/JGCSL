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

// This class will read the roster files contained within the list of archive files and
// return a list of Team objects representing those roster files.
class ReadRosterFilesTask extends ReadSdifArchiveTask<Team>
{
    ReadRosterFilesTask(List<ProcessArchiveItem> archiveFileItems)
    {
        super(archiveFileItems);
    }


    // Read a Roster Archive file and return a Team object representing the team roster.
    // Throw an IOException if there are errors reading from the roster archive file.
    // Throw an SdifException if the roster file contents are not valid.
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

        Team team;
        try {
            team = readRosterFile(rosterFilePath, rosterFileTypeExpected);
        }
        finally {
            if (rosterFileIsTemp) {
                Files.deleteIfExists(Paths.get(rosterFilePath));
            }
        }
        return team;
    }


    private Optional<Team> processRosterRecs(List<SdifRec> rosterRecs, SdifReader.SdifFileFormat fileFormat) throws SdifException
    {
        Optional<Team> optTeam = Optional.empty();
        Team team;

        for (SdifRec rec : rosterRecs) {
            switch (rec.getType()) {
                case TEAM_ID_REC: {
                    if (optTeam.isPresent()) {
                        throw new SdifException("More than 1 team defined in the roster file");
                    }
                    // HY3 has different format than CL2 and SD3
                    team = (fileFormat == SdifReader.SdifFileFormat.HY3 ? Team.fromHy3Data(rec)
                                                                        : Team.fromSdifData(rec));
                    optTeam = Optional.of(team);
                    break;
                }

                case INDIVIDUAL_ADMIN_REC: {
                    // HY3 has different format than CL2 and SD3
                    Athlete athlete = (fileFormat == SdifReader.SdifFileFormat.HY3 ? Athlete.fromHy3(rec)
                            : Athlete.fromSdif(rec));

                    optTeam.orElseThrow( () -> new SdifException("Athlete rec before team rec.") )
                           .addAthlete(athlete);
                    break;
                }

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


    // Read a roster file (CL2, HY3 or SD3) and create a Team object from the roster file data.
    // Throw an SdifException if the roster file is not valid.
    private Team readRosterFile(String rosterFilePath, SdifFileDescription.SdifFileType fileTypeExpected) throws SdifException
    {
        SdifReader sdifReader = new SdifReader(rosterFilePath);
        SdifReader.SdifFileFormat fileFormat = sdifReader.getFileFormat();
        SdifFileDescription.SdifFileType fileType = sdifReader.getFileDescription().getFileType();

        if (fileType != fileTypeExpected) {
            throw new SdifException("File type mismatch, expected " + fileTypeExpected + ", found " + fileType);
        }

        List<SdifRec>  recs = sdifReader.readFile();
        Optional<Team> team = processRosterRecs(recs, fileFormat);

        return team.orElseThrow( () -> new SdifException("No team defined in the SDIF file.") );
    }
}
