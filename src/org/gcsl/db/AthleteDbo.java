package org.gcsl.db;

import org.gcsl.model.Athlete;
import org.gcsl.util.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AthleteDbo {

    // find - find the athlete in the DB.  Return null if not found.
    //        NOTE: Athlete index is on name+dob+gender+teamId
    public static Athlete find(Connection db, Athlete athlete) throws SQLException
    {
        Athlete athleteFromDb = null;
        String  query = "SELECT * from Athletes WHERE name=? AND dob=? AND gender=? AND team_id=?";
        int     teamId = athlete.getTeamId();

        try (PreparedStatement pstmt = db.prepareStatement(query)){

            pstmt.setString(1, athlete.getName());
            pstmt.setString(2, athlete.getDob());
            pstmt.setString(3, athlete.getGender());
            pstmt.setInt(4, teamId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String dob = rs.getString("dob");
                String gender = rs.getString("gender");
                int    id = rs.getInt("id");
                String name = rs.getString("name");
                String lastUpdate = rs.getString("last_update");

                athleteFromDb = new Athlete(id, name, gender, dob, teamId, lastUpdate);
            }
        }

        return athleteFromDb;
    }

    // find an athlete in the DB based on the athlete ID
    public static Athlete find(Connection db, int athleteId) throws SQLException
    {
        Athlete athleteFromDb = null;
        String  query = "SELECT * from Athletes WHERE id=?";

        try (PreparedStatement pstmt = db.prepareStatement(query)){
            pstmt.setInt(1, athleteId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String dob = rs.getString("dob");
                String gender = rs.getString("gender");
                int    id = rs.getInt("id");
                String name = rs.getString("name");
                String lastUpdate = rs.getString("last_update");
                int    teamId = rs.getInt("team_id");

                athleteFromDb = new Athlete(id, name, gender, dob, teamId, lastUpdate);
            }
        }

        return athleteFromDb;
    }

    // find - find the athlete in the DB.  Return null if not found.
    public static Athlete findByNameOrId(Connection db, Athlete athlete) throws SQLException
    {
        Athlete athleteFromDb = null;
        int     teamId = athlete.getTeamId();

        // If we have a valid ID, use that.  Make sure the team is the same as expected
        if (athlete.getId() != Utils.INVALID_ID) {
            athleteFromDb = find(db, athlete.getId());

            if (athleteFromDb != null  &&  athleteFromDb.getTeamId() != teamId) {
                athleteFromDb = null;
            }
        }
        else {
            String  query = "SELECT * from Athletes WHERE name=? AND gender=? AND team_id=?";
            try (PreparedStatement pstmt = db.prepareStatement(query)){

                pstmt.setString(1, athlete.getName());
                pstmt.setString(2, athlete.getGender());
                pstmt.setInt(3, teamId);

                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String dob    = rs.getString("dob");
                    String gender = rs.getString("gender");
                    int    id     = rs.getInt("id");
                    String name   = rs.getString("name");
                    String lastUpdate = rs.getString("last_update");

                    athleteFromDb = new Athlete(id, name, gender, dob, teamId, lastUpdate);
                }
            }
        }

        return athleteFromDb;
    }


    // get the meets ID associated with this athlete
    public static List<Integer> getMeetIds(Connection db, Athlete athlete) throws SQLException
    {
        List<Integer> meetIds = new ArrayList<>();
        String sql = "SELECT meet_id FROM Athlete_Meet WHERE athlete_id = ?";

        try (PreparedStatement pstmt = db.prepareStatement(sql)) {

            pstmt.setInt(1, athlete.getId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                meetIds.add( rs.getInt("meet_id") );
            }
            rs.close();
        }

        return meetIds;
    }


    // insert a single athlete into the DB.
    public static void insert(Connection db, Athlete athlete) throws SQLException
    {
        String sql = "INSERT INTO Athletes (name, dob, gender, team_id) VALUES ( ?, ?, ?, ? )";

        try (PreparedStatement pstmt = db.prepareStatement(sql)){
            pstmt.setString(1, athlete.getName());
            pstmt.setString(2, athlete.getDob());
            pstmt.setString(3, athlete.getGender());
            pstmt.setInt(4, athlete.getTeamId());

            pstmt.executeUpdate();
        }
    }

    // insert a set of athletes into the DB.
    public static void insert(Connection db, Set<Athlete> athletes) throws SQLException
    {
        String sql = "INSERT INTO Athletes (name, dob, gender, team_id) VALUES ( ?, ?, ?, ? )";

        try (PreparedStatement pstmt = db.prepareStatement(sql)){
            for (Athlete athlete : athletes) {
                pstmt.setString(1, athlete.getName());
                pstmt.setString(2, athlete.getDob());
                pstmt.setString(3, athlete.getGender());
                pstmt.setInt(4, athlete.getTeamId());

                pstmt.executeUpdate();
            }
        }
    }

    public static void remove(Connection db, Athlete athlete) throws SQLException
    {
        String sql = "DELETE FROM Athletes WHERE id = ?";

        try (PreparedStatement pstmt = db.prepareStatement(sql)){
            pstmt.setInt(1, athlete.getId());
            pstmt.executeUpdate();
        }
    }
}
