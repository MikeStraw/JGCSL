package org.gcsl;

import org.gcsl.model.*;
import org.gcsl.sdif.SdifException;
import org.gcsl.sdif.SdifFileDescription;
import org.gcsl.sdif.SdifReader;
import org.gcsl.sdif.SdifRec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;


class ReadResultFilesTask extends ReadSdifArchiveTask<MeetResults>
{
    ReadResultFilesTask(List<ProcessArchiveItem> archiveFileItems)
    {
        super(archiveFileItems);
    }


    // Read a Meet Result Archive file and return a MeetResult object.
    // Throw an IOException if there are errors reading from the result archive file.
    // Throw an SdifException if the result file contents are not valid.
    @Override
    MeetResults processArchiveItem(ProcessArchiveItem archiveItem) throws SdifException, IOException
    {
        String  archiveFilePath  = archiveItem.getDirectory() + File.separator + archiveItem.getName();
        String  resultFilePath   = extractSdifFileFromArchive(archiveItem);
        boolean resultFileIsTemp = ! archiveFilePath.equals(resultFilePath);  // not equal means is a temp file

        MeetResults meetResults = readResultFile(resultFilePath, archiveItem.getScenarioType());
        if (resultFileIsTemp) {
            Files.deleteIfExists(Paths.get(resultFilePath));
        }

        return meetResults;
    }


    private Optional<MeetResults> processMeetResults(List<SdifRec> resultRecs) throws SdifException
    {
        Optional<MeetResults> optResults = Optional.empty();
        Optional<Team> optTeam = Optional.empty();

        for (SdifRec rec : resultRecs) {
            switch (rec.getType()) {
                case MEET_REC: {
                    MeetInfo meetInfo = MeetInfo.fromSdif(rec);
                    optResults = Optional.of( new MeetResults(meetInfo, ProcessArchiveItem.Scenario.MEET_RESULTS) );
                    break;
                }
                case TEAM_ID_REC: {
                    optTeam = Optional.of(Team.fromSdifData(rec));
                    optResults.orElseThrow( () -> new SdifException("Meet information required before team information."))
                              .addTeam(optTeam.get());
                    break;
                }
                case INDIVIDUAL_EVENT_REC:   // fall through
                case RELAY_NAME_REC: {
                    Athlete athlete = Athlete.fromSdif(rec);
                    optTeam.orElseThrow( () -> new SdifException("Team information required before athlete information."))
                           .addAthlete(athlete);
                    break;
                }
                default:
                    break;  // don't care about others
            }
        }
        return optResults;
    }


    // Read a result file (CL2, HY3 or SD3) and create a Meet object representing the results of the meet.
    // Throw an SdifException if the result file is not valid.
    private MeetResults readResultFile(String resultFilePath,
                                       ProcessArchiveItem.Scenario scenario) throws SdifException
    {
        SdifFileDescription.SdifFileType fileTypeExpected;

        switch (scenario) {
            case MEET_RESULTS:     { fileTypeExpected = SdifFileDescription.SdifFileType.MEET_RESULTS;  break;}
            case BYE_WEEK_ENTRIES: { throw new RuntimeException("Do not support Bye Week Entries Yet.");}
            case BYE_WEEK_RESULTS: { throw new RuntimeException("Do not support Bye Week Results Yet."); }
            case RAIN_OUT_ENTRIES: { throw new RuntimeException("Do not support Rain Out Entries Yet."); }
            case RAIN_OUT_RESULTS: { throw new RuntimeException("Do not support Rain Out Results Yet."); }
            default:               { throw new RuntimeException("Invalid meet results scenario."); }
        }

        SdifReader sdifReader = new SdifReader(resultFilePath);
        List<SdifRec> sdifRecs = readResultFile(sdifReader, fileTypeExpected);
        Optional<MeetResults> results = processMeetResults(sdifRecs);
        results.orElseThrow( () -> new SdifException("No results defined in the SDIF file.") )
               .setResultFileDate(sdifReader.getFileDescription().getFileDate());

        return results.get();
    }


    private List<SdifRec> readResultFile(SdifReader reader, SdifFileDescription.SdifFileType expectedSdifType) throws SdifException
    {
        SdifFileDescription.SdifFileType actualSdifType = reader.getFileDescription().getFileType();

        if (actualSdifType != expectedSdifType) {
            throw new SdifException("Result file was of type " + actualSdifType +
                                    ", exptected type of " + expectedSdifType);
        }

        return reader.readFile();
    }
}
