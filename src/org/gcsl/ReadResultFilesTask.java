package org.gcsl;

import javafx.concurrent.Task;
import org.gcsl.model.*;
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


public class ReadResultFilesTask extends Task<List<MeetResults>>
{
    private List<ProcessArchiveItem> archiveItems = Collections.emptyList();

    ReadResultFilesTask(List<ProcessArchiveItem> archiveFileItems)
    {
        archiveItems = archiveFileItems;
    }

    @Override
    protected List<MeetResults> call() throws Exception
    {
        List<MeetResults> results = new Vector<>();

        int curItem  = 0;
        int numItems = archiveItems.size();
        System.out.printf("Inside ReadResultFilesTask::call(). archiveItems.size()=%d %n", archiveItems.size());

        for (ProcessArchiveItem archiveItem : archiveItems)
        {
            if (isCancelled())  { break; }
            curItem++;

            try {
                MeetResults result = readResultArchive(archiveItem);
                results.add(result);
            }
            catch (SdifException | IOException e) {
                updateMessage("Read results file task failed ..." + e.getMessage());
                return null;
            }

            updateMessage("Processing archive: " + archiveItem.getName());
            updateProgress(curItem, numItems);
        }
        updateMessage("Result files read successfully.");

        return results;
    }


    // Extracts the results file from the archive.  Return the path to the extracted archive file.
    private String extractResultsArchiveFile(ProcessArchiveItem archiveItem) throws IOException, SdifException
    {
        String []archiveContents = orderResultFiles(archiveItem.getContents());
        String   archiveFilePath = archiveItem.getDirectory() + File.separator + archiveItem.getName();
        String   resultFilePath;

        // Extract the results file from the archive
        Utils.ARCHIVE_FILE_TYPE fileType = Utils.getArchiveFileType(archiveFilePath);
        switch (fileType) {
            case ZIP : resultFilePath = Utils.getFileFromArchive(archiveFilePath, archiveContents[0]);
                break;
            case SD3:  resultFilePath = archiveFilePath;
                break;
            default:   throw new SdifException("Unknown roster file archive.  Filepath=" + archiveFilePath);
        }

        return resultFilePath;
    }

    // Order the archive content based on whether to prioritize .CL2 files over .HY3 files.
    private String [] orderResultFiles(String archiveContents)
    {
        // TODO: really should check a config entry, for now just assume that we want .CL2 entries before .HY3
        return archiveContents.split(", ");
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



    // Read a Meet Result Archive file and return a MeetResult object.
    // Throw an IOException if there are errors reading from the result archive file.
    // Throw an SdifException if the result file contents are not valid.
    private MeetResults readResultArchive(ProcessArchiveItem archiveItem) throws SdifException, IOException
    {
        String  archiveFilePath  = archiveItem.getDirectory() + File.separator + archiveItem.getName();
        String  resultFilePath   = extractResultsArchiveFile(archiveItem);
        boolean resultFileIsTemp = ! archiveFilePath.equals(resultFilePath);  // not equal means is a temp file

        MeetResults meetResults = readResultFile(resultFilePath, archiveItem.getScenarioType());
        if (resultFileIsTemp) {
            Files.deleteIfExists(Paths.get(resultFilePath));
        }

        return meetResults;
    }

    // Read a result file (CL2, HY3 or SD3) and create a Meet object representing the results of the meet.
    // Throw an SdifException if the result file is not valid.
    private MeetResults readResultFile(String resultFilePath,
                                       ProcessArchiveItem.Scenario scenario) throws SdifException
    {
        SdifFileDescription.SdifFileType fileType= SdifFileDescription.SdifFileType.UNKNOWN;
        SdifReader sdifReader = new SdifReader(resultFilePath);

        switch (scenario) {
            case MEET_RESULTS:     { fileType = SdifFileDescription.SdifFileType.MEET_RESULTS;  break;}
            case BYE_WEEK_RESULTS: { break; }
            case BYE_WEEK_ENTRIES: { break; }
            case RAIN_OUT_ENTRIES: { break; }
            case RAIN_OUT_RESULTS: { break; }
            default:               { throw new SdifException("Invalid meet results scenario."); }
        }

        List<SdifRec> sdifRecs = readResultFile(sdifReader, fileType);
        Optional<MeetResults> results = processMeetResults(sdifRecs);

        return results.orElseThrow( () -> new SdifException("No results defined in the SDIF file.") );
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
