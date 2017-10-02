package org.gcsl.db;

import org.gcsl.model.Athlete;

import java.sql.*;
import java.util.List;

public class OrphanDbo
{
    public static void insert(Connection db, List<Athlete> orphans, int meetId) throws SQLException
    {
        String sql = "INSERT INTO Orphans (team_id, name, dob, gender, meet_id) VALUES ( ?, ?, ?, ?, ? )";

        try (PreparedStatement pstmt = db.prepareStatement(sql)) {

            for (Athlete orphan : orphans) {
                pstmt.setInt(1, orphan.getTeamId());
                pstmt.setString(2, orphan.getName());
                pstmt.setString(3, orphan.getDob());
                pstmt.setString(4, orphan.getGender());
                pstmt.setInt(5, meetId);

                pstmt.executeUpdate();
            }
        }
    }
}
