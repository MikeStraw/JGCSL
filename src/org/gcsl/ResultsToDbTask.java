package org.gcsl;

import javafx.concurrent.Task;
import org.gcsl.db.AthleteDbo;
import org.gcsl.db.MeetDbo;
import org.gcsl.db.OrphanDbo;
import org.gcsl.db.TeamDbo;
import org.gcsl.model.*;
import org.gcsl.util.Utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class ResultsToDbTask extends Task<Void>
{
    private Connection dbConn;
    private List<MeetResults> meetResults;

    ResultsToDbTask(Connection dbConn, List<MeetResults> meetResults)
    {
        this.dbConn = dbConn;
        this.meetResults = meetResults;
    }

    @Override
    protected Void call() throws Exception
    {
        int curItem = 0;
        int existingMeetId = Utils.INVALID_ID;
        int numItems = meetResults.size();
        System.out.printf("Inside ResultsToDbTask, meetResults.size()=%d. %n", meetResults.size());

        for (MeetResults meet : meetResults) {
            if (isCancelled()) {
                break;
            }

            // Add bye week team if necessary
            if (meet.getResultsScenario() == ProcessArchiveItem.Scenario.BYE_WEEK_ENTRIES
            ||  meet.getResultsScenario() == ProcessArchiveItem.Scenario.BYE_WEEK_RESULTS) {
                Team byeWeekTeam = TeamDbo.findByCode(dbConn, "BYE");
                if (byeWeekTeam != null) {
                    meet.addTeam(byeWeekTeam);
                }
            }

            curItem++;
            updateMessage("Processing results for meet: " + meet.getName());
            updateProgress(curItem, numItems);

            // NOTE:  different result types may only have 1 team
            List<Team> teams = meet.getTeams();
            if (teams.size() < 2)  {
                System.err.println("Error:  Not 2 teams in the meet results.");
                updateMessage("Error:  Not 2 teams in the meet results.");
                return null;
            }

            Meet dbMeet = MeetDbo.findByTeams(dbConn, meet);
            if (dbMeet != null) {
                existingMeetId = dbMeet.getId();
                removeResults(dbMeet);
            }

            if (! updateTeamIds(teams)) {
                System.err.println("Error:  one of the teams in the results is not found in the DB.");
                updateMessage("Error:  one of the teams in the results is not found in the DB.");
                return null;
            }
            List<Athlete> orphans = updateAthlteIds(teams);
            meet.addOrphans(orphans);
            System.out.printf("Inserting meet into the DB with %d orphans %n", orphans.size());

            if (existingMeetId != Utils.INVALID_ID) {
                MeetDbo.update(dbConn, meet, existingMeetId);
                System.out.printf("Updated meet %d in the DB.\n", existingMeetId);
            }
            else {
                int meetId = MeetDbo.insert(dbConn, meet);
                System.out.printf("Inserted meet %d into the DB.\n", meetId);
            }
        }

        updateMessage("Successfully processed " + meetResults.size() + " meet results to the DB.");
        return null;
    }


    // Remove the results associated with a meet.
    private void removeResults(Meet meet) throws SQLException
    {
        MeetDbo.removeResults(dbConn, meet.getId());
        OrphanDbo.removeOrphans(dbConn, meet.getId());
    }


    // Update the athletes with their DB IDs.  Any athletes not found in the DB are returned
    // in the orphan list.
    private List<Athlete> updateAthlteIds(List<Team> teams) throws SQLException
    {
        List<Athlete> orphans = new ArrayList<>();

        for (Team resultsTeam : teams) {
            List<Athlete> newOrphans = new ArrayList<>();

            for (Athlete athlete : resultsTeam.getAthletes()) {
                Athlete dbAthlete = AthleteDbo.find(dbConn, athlete);
                if (dbAthlete == null) {
                    newOrphans.add(athlete);
                }
                else {
                    athlete.setId(dbAthlete.getId());
                }
            }

            for (Athlete orphan : newOrphans) {
                orphans.add(orphan);
                resultsTeam.removeAthlete(orphan);
            }
        }
        return orphans;
    }

    // Update the teams with their DB IDs
    private boolean updateTeamIds(List<Team> teams) throws SQLException
    {
        boolean rc = true;

        for (Team resultTeam : teams) {
            Team dbTeam = TeamDbo.find(dbConn, resultTeam);
            if (dbTeam == null) {
                System.err.println("Error:  team " + resultTeam.getName() + " is not found in the DB.");
                rc = false;
            }
            else {
                resultTeam.setId(dbTeam.getId());
            }
        }
        return rc;
    }
}
