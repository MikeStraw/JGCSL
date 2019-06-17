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

        MeetResults meetResults;
        try {
            meetResults = readResultFile(resultFilePath, archiveItem.getScenarioType());
        }
        finally {
            if (resultFileIsTemp) {
                Files.deleteIfExists(Paths.get(resultFilePath));
            }
        }
        return meetResults;
    }

    private Optional<MeetResults> processMeetEntries(List<SdifRec> entryRecs,
                                                     ProcessArchiveItem.Scenario scenario,
                                                     SdifReader.SdifFileFormat fileFormat) throws SdifException
    {
        Optional<MeetResults> optResults = Optional.empty();
        Optional<Team> optTeam = Optional.empty();

        for (SdifRec rec : entryRecs) {
            switch (rec.getType()) {
                case MEET_REC: {
                    // HY3 has different format than CL2 and SD3
                    MeetInfo meetInfo = (fileFormat == SdifReader.SdifFileFormat.HY3 ? MeetInfo.fromHy3(rec)
                                                                                     : MeetInfo.fromSdif(rec));
                    optResults = Optional.of(new MeetResults(meetInfo, scenario));
                    break;
                }
                case TEAM_ID_REC: {
                    // HY3 has different format than CL2 and SD3
                    Team team = (fileFormat == SdifReader.SdifFileFormat.HY3 ? Team.fromHy3Data(rec)
                                                                             : Team.fromSdifData(rec));
                    optTeam = Optional.of(team);
                    optResults.orElseThrow( () -> new SdifException("Meet information required before team information."))
                            .addTeam(optTeam.get());
                    break;
                }
                case INDIVIDUAL_ADMIN_REC: {
                    // HY3 has different format than CL2 and SD3
                    Athlete athlete = (fileFormat == SdifReader.SdifFileFormat.HY3 ? Athlete.fromHy3(rec)
                            : Athlete.fromSdif(rec));

                    optTeam.orElseThrow( () -> new SdifException("Team information required before athlete information.") )
                           .addAthlete(athlete);
                    break;
                }
                case INDIVIDUAL_EVENT_REC: // fall through
                case RELAY_NAME_REC:  {
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

    // TODO - Could we pass scenario to processMeetResults and use this for both meet results and meet entries?
    //        Pretty much everything is the same.  Meet results do a little extra checking for No Shows, but
    //        that shouldn't matter.

    private Optional<MeetResults> processMeetResults(List<SdifRec> resultRecs,
                                                     ProcessArchiveItem.Scenario resultScenario) throws SdifException
    {
        Optional<MeetResults> optResults = Optional.empty();
        Optional<Relay> optRelay = Optional.empty();
        Optional<Team> optTeam = Optional.empty();

        for (SdifRec rec : resultRecs) {
            switch (rec.getType()) {
                case MEET_REC: {
                    MeetInfo meetInfo = MeetInfo.fromSdif(rec);
                    optResults = Optional.of( new MeetResults(meetInfo, resultScenario) );
                    break;
                }
                case TEAM_ID_REC: {
                    optTeam = Optional.of(Team.fromSdifData(rec));
                    optResults.orElseThrow( () -> new SdifException("Meet information required before team information."))
                              .addTeam(optTeam.get());

                    optRelay = Optional.empty();  // reset indicator when reading the team record
                    break;
                }
                case INDIVIDUAL_EVENT_REC: {
                    Athlete athlete = Athlete.fromSdifMeetResult(rec);
                    if (! athlete.isNoShow()) {
                        optTeam.orElseThrow( () -> new SdifException("Team information required before athlete information."))
                               .addAthlete(athlete);
                    }

                    break;
                }
                case RELAY_EVENT_REC: {
                    // Defines tha name of a relay and the relay's result (ie, time or no-show)
                    optRelay = Optional.of(Relay.fromSdifData(rec));
                    break;
                }
                case RELAY_NAME_REC: {
                    // make sure relay has a result before adding athlete
                    Athlete athlete = Athlete.fromSdif(rec);
                    Relay relay = optRelay.orElseThrow( () -> new SdifException("Relay event information required before relay name information.") );
                    if (! relay.isNoShow()) {
                        relay.addAthlete(athlete);
                        optTeam.orElseThrow( () -> new SdifException("Team information required before athlete information."))
                               .addAthlete(athlete);
                    }
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
        Optional<MeetResults> results;

        switch (scenario) {
            case MEET_RESULTS:     { fileTypeExpected = SdifFileDescription.SdifFileType.MEET_RESULTS;    break; }
            case BYE_WEEK_ENTRIES: { fileTypeExpected = SdifFileDescription.SdifFileType.VENDOR_DEFINED;  break; }
            case BYE_WEEK_RESULTS: { fileTypeExpected = SdifFileDescription.SdifFileType.MEET_RESULTS;    break; }
            case RAIN_OUT_ENTRIES: { fileTypeExpected = SdifFileDescription.SdifFileType.VENDOR_DEFINED;  break; }
            case RAIN_OUT_RESULTS: { fileTypeExpected = SdifFileDescription.SdifFileType.MEET_RESULTS;    break; }
            default:               { throw new RuntimeException("Invalid meet results scenario."); }
        }

        SdifReader sdifReader = new SdifReader(resultFilePath);
        SdifReader.SdifFileFormat fileFormat = sdifReader.getFileFormat();

        List<SdifRec> sdifRecs = readResultFile(sdifReader, fileTypeExpected);
        if (scenario == ProcessArchiveItem.Scenario.BYE_WEEK_ENTRIES
        ||  scenario == ProcessArchiveItem.Scenario.RAIN_OUT_ENTRIES) {
            results = processMeetEntries(sdifRecs, scenario, fileFormat);
        }
        else {
            results = processMeetResults(sdifRecs, scenario);
        }

        results.orElseThrow( () -> new SdifException("No results defined in the SDIF file.") )
               .setResultFileDate(sdifReader.getFileDescription().getFileDate());

        return results.get();
    }


    private List<SdifRec> readResultFile(SdifReader reader, SdifFileDescription.SdifFileType expectedSdifType) throws SdifException
    {
        SdifFileDescription.SdifFileType actualSdifType = reader.getFileDescription().getFileType();

        if (actualSdifType != expectedSdifType) {
            System.out.println("Result file was of type " + actualSdifType +
                                       ", expected type of " + expectedSdifType);
            // TODO:  where is this exception caught????
            throw new SdifException("Result file was of type " + actualSdifType +
                                    ", expected type of " + expectedSdifType);
        }

        return reader.readFile();
    }
}
