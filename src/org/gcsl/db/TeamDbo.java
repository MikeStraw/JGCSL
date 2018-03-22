package org.gcsl.db;

import org.gcsl.model.Athlete;
import org.gcsl.model.Team;
import org.gcsl.util.Utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TeamDbo {


    // Find the team in the DB by either team code or team ID
    public static Team find(Connection db, Team team) throws SQLException
    {
        if (team.getId() != Utils.INVALID_ID)  return findById(db, team.getId());
        else                                   return findByCode(db, team.getCode());
    }


    // Find all the teams in the DB.
    public static List<Team> findAll(Connection db)
    {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM Teams";

        try (Statement stmt = db.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String code = rs.getString("code");
                int    id   = rs.getInt("id");
                String name = rs.getString("name");
                String lastUpdate = rs.getString("last_update");

                teams.add(new Team(id, code, name, lastUpdate));
            }
        }
        catch (SQLException e) {
            System.err.println("ERROR - finding all teams.");
            e.printStackTrace();
        }

        return teams;
    }


    // Find the team in the DB based on the team code.  Returns null if not in the DB.
    // This method does not retrieve the athletes associated with the team.
    // Throw an SQLException if there is a DB error
    public static Team findByCode(Connection db, String teamCode) throws SQLException
    {
        Team   dbTeam = null;
        String sql = "SELECT * FROM Teams WHERE Code = ?";

        try (PreparedStatement pstmt = db.prepareStatement(sql)) {
             pstmt.setString(1, teamCode);
             ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String code = rs.getString("code");
                int    id   = rs.getInt("id");
                String name = rs.getString("name");
                String lastUpdate = rs.getString("last_update");

                dbTeam = new Team(id, code, name, lastUpdate);
            }
        }

        return dbTeam;
    }


    // Find the team in the DB based on the team ID.  Returns null if not in the DB.
    // This method does not retrieve the athletes associated with the team.
    // Throw an SQLException if there is a DB error
    public static Team findById(Connection db, int teamId) throws SQLException
    {
        Team team = null;
        String sql = "SELECT * FROM Teams WHERE id = ?";

        if (teamId != Utils.INVALID_ID) {
            try (PreparedStatement pstmt = db.prepareStatement(sql)){
                pstmt.setInt(1, teamId);

                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String code = rs.getString("code");
                    int    id   = rs.getInt("id");
                    String name = rs.getString("name");
                    String lastUpdate = rs.getString("last_update");

                    team = new Team(id, code, name, lastUpdate);
                }
            }
        }

        return team;
    }


    // Insert a team into the DB.  Return the inserted team which now contains an ID.
    // Throw SQLException if there is an SQL error.
    public static Team insert(Connection db, Team team) throws SQLException
    {
        String sql = "INSERT INTO Teams (name, code) VALUES ( ?, ? )";
//        System.out.println("TeamDbo::insert - " + sql);
        try (PreparedStatement pstmt = db.prepareStatement(sql)) {
            pstmt.setString(1, team.getName());
            pstmt.setString(2, team.getCode());
            pstmt.executeUpdate();

            return find(db, team);
        }
    }


    public static Set<Athlete> retrieveAthletes(Connection db, Team team) throws SQLException
    {
        Set<Athlete> athletes = new HashSet<>();
        String sql = "SELECT * FROM Athletes WHERE team_id = ?";
        int teamId = team.getId();
        System.out.printf("TeamDbo::retrieveAthletes for team %d %n", teamId);

        if (teamId != Utils.INVALID_ID) {
            try (PreparedStatement pstmt = db.prepareStatement(sql)){
                pstmt.setInt(1, teamId);

                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String dob    = rs.getString("dob");
                    String gender = rs.getString("gender");
                    int    id     = rs.getInt("id");
                    String name   = rs.getString("name");
                    String lastUpdate = rs.getString("last_update");

                    athletes.add(new Athlete(id, name, gender, dob, teamId, lastUpdate));
                }
            }
        }
//        System.out.printf("TeamDbo::retrieveAthletes found %d athletes in DB. %n", athletes.size() );
        return athletes;
    }
}
