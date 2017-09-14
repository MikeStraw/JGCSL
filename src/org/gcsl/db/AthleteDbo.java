package org.gcsl.db;

import org.gcsl.model.Athlete;

import java.sql.*;

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
}
