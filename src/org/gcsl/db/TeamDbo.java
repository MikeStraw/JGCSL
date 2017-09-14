package org.gcsl.db;

import org.gcsl.model.Team;
import java.sql.*;

public class TeamDbo {

    // find - Find the team in the DB.  Returns null if not in the DB.
    // Throw an SQLException if there is a DB error
    public static Team find(Connection db, Team team) throws SQLException
    {
        Team   dbTeam = null;
        String query = "SELECT * FROM Teams WHERE code = '" + team.getCode() + "'";
        System.out.println("TeamDbo::find - " + query);

        try (Statement stmt = db.createStatement();
             ResultSet rs = stmt.executeQuery(query)){

            if (rs.next()) {
                String code = rs.getString("code");
                int    id = rs.getInt("id");
                String name = rs.getString("name");
                String lastUpdate = rs.getString("last_update");

                dbTeam = new Team(id, code, name, lastUpdate);
            }
        }

        return dbTeam;
    }

    // Insert a team into the DB.  Return the inserted team which now contains an ID.
    // Throw SQLException if there is an SQL error.
    public static Team insert(Connection db, Team team) throws SQLException
    {
        String sql = "INSERT INTO Teams (name, code) VALUES ( ?, ? )";
        System.out.println("TeamDbo::insert - " + sql);
        try (PreparedStatement pstmt = db.prepareStatement(sql)) {
            pstmt.setString(1, team.getName());
            pstmt.setString(2, team.getCode());
            pstmt.executeUpdate();

            return find(db, team);
        }
    }
}
