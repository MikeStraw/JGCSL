package org.gcsl;

import javafx.concurrent.Task;
import org.gcsl.db.AthleteDbo;
import org.gcsl.db.MeetDbo;
import org.gcsl.db.TeamDbo;
import org.gcsl.model.Athlete;
import org.gcsl.model.Meet;
import org.gcsl.model.MeetResults;
import org.gcsl.model.Team;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

public class ResultsToDbTask extends Task<Void>
{
    private Connection dbConn;
    private List<MeetResults> meetResults = null;

    ResultsToDbTask(Connection dbConn, List<MeetResults> meetResults)
    {
        this.dbConn = dbConn;
        this.meetResults = meetResults;
    }

    @Override
    protected Void call() throws Exception
    {
        int curItem = 0;
        int numItems = meetResults.size();
        System.out.printf("Inside ResultsToDbTask, meetResults.size()=%d. %n", meetResults.size());

        for (MeetResults meet : meetResults) {
            if (isCancelled()) {
                break;
            }
            curItem++;

            // NOTE:  different result types may only have 1 team
            List<Team> teams = meet.getTeams();
            if (teams.size() < 2) {
                System.err.println("Error:  Not 2 teams in the meet results.");
                updateMessage("Error:  Not 2 teams in the meet results.");
                break;
            }

            Meet dbMeet = MeetDbo.findByTeams(dbConn, meet);
            if (dbMeet != null) {
                // Found existing results - Probably should remove them and continue
                System.err.println("Error:  Meet already found in DB... exiting for now. ");
                updateMessage("Error:  Meet already found in DB... exiting for now. ");
                return null;
            }

            if (! updateTeamIds(teams)) {
                System.err.println("Error:  one of the teams in the results is not found in the DB.");
                updateMessage("Error:  one of the teams in the results is not found in the DB.");
                return null;
            }
            List<Athlete> orphans = updateAthlteIds(teams);
            meet.addOrphans(orphans);

            System.out.printf("Inserting meet into the DB with %d orphans %n", orphans.size());
            int meetId = MeetDbo.insert(dbConn, meet);
            System.out.printf("Inserted meet %d into the DB.", meetId);

            updateMessage("Processing results for meet: " + meet.getName());
            updateProgress(curItem, numItems);
        }

        updateMessage("Successfully processed " + meetResults.size() + " meet results to the DB.");
        return null;
    }

    // Update the athletes with their DB IDs.  Any athletes not found in the DB are returned
    // in the orphan list.
    private List<Athlete> updateAthlteIds(List<Team> teams) throws SQLException
    {
        List<Athlete> orphans = new Vector<>();

        for (Team resultsTeam : teams) {
            List<Athlete> newOrphans = new Vector<>();

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
