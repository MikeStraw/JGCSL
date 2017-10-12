package org.gcsl.db;

import org.gcsl.model.*;
import org.gcsl.util.Utils;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

public class MeetDbo
{
    // Find the meet in the DB based on the meetId.
    // Returns null if meet is not found.
    public static Meet findById(Connection db, int meetId) throws SQLException
    {
        Meet meet = null;
        String sql = "SELECT * FROM Meets WHERE id = ?";

        if (meetId != Utils.INVALID_ID) {
            try (PreparedStatement pstmt = db.prepareStatement(sql)){
                pstmt.setInt(1, meetId);

                ResultSet rs = pstmt.executeQuery();
                meet = makeMeetFromResultSet(rs);
            }
        }

        return meet;
    }


    // Find the meet associated with the teams contained in the meet result.  If the meet is not found
    // in the DB, return null
    public static Meet findByTeams(Connection conn, MeetResults meetResult) throws SQLException
    {
        Meet meet = null;
        List<Team> teams = meetResult.getTeams();

        if (teams.size() >= 2) {
            Team team1 = teams.get(0);
            Team team2 = teams.get(1);
            int team1Id = team1.getId();
            int team2Id = team2.getId();
            LocalDate meetDate = meetResult.getDate();

            if (team1Id != Utils.INVALID_ID &&  team2Id != Utils.INVALID_ID) {
                meet = findMeetByTeamIds(conn, team1Id, team2Id, meetDate);
            }
            else {
                meet = findMeetByTeamCodes(conn, team1.getCode(), team2.getCode(), meetDate);
            }
        }
        return meet;
    }


    // Insert the meet information, athlete-meet and orphan data into the DB.
    // Return the ID of the meet in the DB.
    public static int insert(Connection db, MeetResults meet) throws SQLException
    {
        int meetId = insertMeet(db, meet);
        OrphanDbo.insert(db, meet.getOrphans(), meetId);
        creditAthletesForMeet(db, meet, meetId);

        return meetId;
    }


    // Remove the athletes associated with a particular meet
    public static void removeResults(Connection db, int meetId) throws SQLException
    {
        String sql = "DELETE FROM Athlete_Meet WHERE meet_id = ?";

        if (meetId != Utils.INVALID_ID) {
            try (PreparedStatement pstmt = db.prepareStatement(sql)){
                pstmt.setInt(1, meetId);
                pstmt.executeUpdate();
            }
        }
    }


    // Update the meet information, athlete-meet and orphan data into the DB.
    // NOTE:  only the file_date can be updated.  last_update is automatically updated.
    public static void update(Connection db, MeetResults meet, int meetId) throws SQLException
    {
        String sql = "UPDATE Meets SET file_date = ? WHERE id = ? ";

        try (PreparedStatement pstmt = db.prepareStatement(sql)){
            pstmt.setString(1, meet.getResultsFileDate());
            pstmt.setInt(2, meetId);

            pstmt.executeUpdate();
            OrphanDbo.insert(db, meet.getOrphans(), meetId);
            creditAthletesForMeet(db, meet, meetId);
        }
    }


    // Give all the athletes from the meet credit by creating an entry in the Athlete-Meet table.
    // Throw an SQLException on error.
    private static void creditAthletesForMeet(Connection db, MeetResults meetResults, int meetId) throws SQLException
    {
        if (meetId == Utils.INVALID_ID) {
            System.err.println("WARNING:  meet ID is INVALID, no credit for meet results.");
        }
        else {
            for (Team team : meetResults.getTeams()) {
                insertAthleteMeet(db, team.getAthletes(), meetId);
             }
        }
    }


    private static Meet findMeetByTeamCodes(Connection db, String code1, String code2, LocalDate meetDate) throws SQLException
    {
        Meet meet  = null;
        Team team1 = TeamDbo.findByCode(db, code1);
        Team team2 = TeamDbo.findByCode(db, code2);

        if (team1 != null && team2 != null) {
            meet =  findMeetByTeamIds(db, team1.getId(), team2.getId(), meetDate);
        }

        return meet;
    }


    private static Meet findMeetByTeamIds(Connection db, int id1, int id2, LocalDate meetDate) throws SQLException
    {
        Meet   meet;
        String meetDateStr = meetDate.format( DateTimeFormatter.ofPattern("yyyy-MM-dd") );
        String sql = "SELECT * FROM Meets WHERE team1_id = ? AND team2_id = ? AND meet_date = ?";

        try (PreparedStatement pstmt = db.prepareStatement(sql)){
            pstmt.setInt(1, id1);
            pstmt.setInt(2, id2);
            pstmt.setString(3, meetDateStr);

            ResultSet rs = pstmt.executeQuery();
            meet = makeMeetFromResultSet(rs);
        }
        return meet;
    }


    private static void insertAthleteMeet(Connection db, Set<Athlete> athletes, int meetId) throws SQLException
    {
        String sql = "INSERT INTO Athlete_Meet (athlete_id, meet_id) VALUES ( ?, ? )";

        try (PreparedStatement pstmt = db.prepareStatement(sql)){
            for (Athlete athlete : athletes) {
                pstmt.setInt(1, athlete.getId());
                pstmt.setInt(2, meetId);

                pstmt.executeUpdate();
            }
        }
    }


    // Insert the meet into the Meets table and return the ID of the newly inserted meet.
    // Throws an SQLException on error.
    private static int insertMeet(Connection db, MeetResults meetResults) throws SQLException
    {
        String sql = "INSERT INTO Meets (meet_date, file_date, team1_id, team2_id, result_type) " +
                     "VALUES (?, ?, ?, ?, ?)";
        int    team1Id = meetResults.getTeams().get(0).getId();
        int    team2Id = meetResults.getTeams().get(1).getId();

        try (PreparedStatement pstmt = db.prepareStatement(sql)){
            pstmt.setString(1, meetResults.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            pstmt.setString(2, meetResults.getResultsFileDate());
            pstmt.setInt(3, team1Id);
            pstmt.setInt(4, team2Id);
            pstmt.setString(5, meetResults.getResultsScenario().toString());

            pstmt.executeUpdate();
        }

        Meet dbMeet = findMeetByTeamIds(db, team1Id, team2Id, meetResults.getDate());
        return dbMeet.getId();
    }


    private static Meet makeMeetFromResultSet(ResultSet rs) throws SQLException
    {
        Meet meet = null;
        if (rs.next()) {
            String fileDateStr = rs.getString("file_date");
            int id = rs.getInt("id");
            String meetDateStr = rs.getString("meet_date");
            String lastUpdateStr = rs.getString("last_update");
            String resultType = rs.getString("result_type");
            int team1Id = rs.getInt("team1_id");
            int team2Id = rs.getInt("team2_id");

            meet = new Meet(id, team1Id, team2Id, meetDateStr, resultType, fileDateStr, lastUpdateStr);
        }
        return meet;
    }
}
