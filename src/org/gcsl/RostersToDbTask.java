package org.gcsl;

import javafx.concurrent.Task;
import org.gcsl.db.AthleteDbo;
import org.gcsl.db.TeamDbo;
import org.gcsl.model.Athlete;
import org.gcsl.model.Team;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RostersToDbTask extends Task<Void>
{
    private Connection dbConn;
    private List<Team> teams;

    RostersToDbTask(Connection conn, List<Team> teams)
    {
        this.dbConn = conn;
        this.teams  = teams;
    }


    @Override
    protected Void call() throws Exception
    {
        int curItem = 0;
        int numItems = teams.size();
        System.out.printf("Inside RostersToDbTask, teams.size()=%d. %n", teams.size());

        try {
            dbConn.setAutoCommit(false); // use transactions for performance reasons.

            for (Team team : teams) {
                if (isCancelled()) {
                    break;
                }
                curItem++;

                Team dbTeam = TeamDbo.find(dbConn, team);
                if (dbTeam == null) {
                    // Team not in DB, add it to DB.
                    dbTeam = TeamDbo.insert(dbConn, team);
                } else {
                    // Team in DB, get all the athletes associated with this team.
                    dbTeam.addRoster(TeamDbo.retrieveAthletes(dbConn, dbTeam));
                }
                team.setId(dbTeam.getId());   // now we know the team ID, assign it to the csvTeam

                if (dbTeam.getAthletes().size() == 0) {
                    insertAthletes(team.getAthletes());
                } else {
                    mergeAthletes(dbTeam, team);
                }
                dbConn.commit();

                updateMessage("Processing roster for team: " + team.getName());
                updateProgress(curItem, numItems);
            }
        } catch (SQLException e) {
            try {
                dbConn.rollback();
            } catch (SQLException e1) {
                System.out.println(e1.getMessage());
            }
            updateMessage("Inserting teams/athletes to the DB failed ..." + e.getMessage());
            return null;
        } finally {
            try {
                dbConn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        updateMessage("Successfully processed " + teams.size() + " team to the DB.");
        return null;
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


    // Insert a set of athletes into the DB.
    // Throws an SQLException if there is a DB error.
    private void insertAthletes(Set<Athlete> athletes) throws SQLException
    {
        System.out.printf("Inserting %d athletes into the DB. %n", athletes.size());
        for (Athlete a : athletes) {
            AthleteDbo.insert(dbConn, a);
        }
    }


    // Merge a new team roster with the existing team roster.  New athletes are added
    // to the DB and existing athletes not on the new team are removed from the DB.
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
}
