package org.gcsl;

import org.gcsl.db.AthleteDbo;
import org.gcsl.db.MeetDbo;
import org.gcsl.db.OrphanDbo;
import org.gcsl.db.TeamDbo;
import org.gcsl.model.Athlete;
import org.gcsl.model.Meet;
import org.gcsl.model.Orphan;
import org.gcsl.model.Team;
import org.gcsl.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Reports
{
    private Connection db;
    private File reportDir;

    // cache these maps
    private Map<Integer, String> meetIdToDateMapCache = null;
    private Map<Integer, String> teamIdToNameMapCache = null;


    public Reports(Connection db, File dir)
    {
        this.db = db;
        this.reportDir = dir;
    }


    // Report the number of meets for each athlete
    public void meetCountReport() throws Exception
    {
        String fileSpec = Utils.createFileSpec(reportDir, "meet_count", "csv", true);
        Map<Integer, String> meetIdToDateMap = createMeetIdToDateMap();

        try (PrintWriter pw  = new PrintWriter(fileSpec)) {
            pw.println("Team Name,Athlete Name,Sex,DOB,Count,Meets");

            List<Team> teams = TeamDbo.findAll(db);
            for (Team team : teams) {
                Set<Athlete> athletes = TeamDbo.retrieveAthletes(db, team);

                for (Athlete athlete : athletes) {
                    List<Integer> meetIds = AthleteDbo.getMeetIds(db, athlete);
                    if (meetIds.size() > 0) {
                        printMeetCountData(pw, athlete, team.getCode(), meetIds, meetIdToDateMap);
                    }
                }
            }
        }
    }


    // Report the orphans from the orphan table
    public void orphanReport() throws IOException
    {
        String fileSpec = Utils.createFileSpec(reportDir, "orphan_report", "csv", true);

        try (PrintWriter pw  = new PrintWriter(fileSpec)) {
            printOrphanReport(pw, OrphanDbo.findAll(db));
        }
    }


    private Map<Integer, String> createMeetIdToDateMap()
    {
        if (meetIdToDateMapCache == null) {
            meetIdToDateMapCache = new HashMap<>();
            List<Meet> meets = MeetDbo.findAll(db);

            for (Meet meet : meets) {
                meetIdToDateMapCache.put(meet.getId(), meet.getMeetDate());
            }
        }

        return meetIdToDateMapCache;
    }


    private Map<Integer, String> createTeamIdToNameMap()
    {
        if (teamIdToNameMapCache == null) {
            teamIdToNameMapCache = new HashMap<>();
            List<Team> teams = TeamDbo.findAll(db);

            for (Team team : teams) {
                teamIdToNameMapCache.put(team.getId(), team.getName());
            }
        }

        return teamIdToNameMapCache;
    }


    private String meetIdsToDateString(List<Integer> meetIds, Map<Integer, String> idToDateMap)
    {
        StringBuilder sb = new StringBuilder();

        for (Integer id : meetIds) {
            String date = idToDateMap.get(id);
            if (date != null) {
                sb.append(date).append(" ");
            }
        }

        return sb.toString();
    }


    private void printMeetCountData(PrintWriter pw, Athlete athlete, String teamName, List<Integer>meetIds,
                                    Map<Integer, String> meetIdToDateMap)
    {
        String meetDates = meetIdsToDateString(meetIds, meetIdToDateMap);

        // Team, "Athlete Name", Gender, DOB, Meet Count, Meet Dates ...
        pw.printf("%s,\"%s\",%s,%s,%d,%s%n", teamName, athlete.getName(), athlete.getGender(), athlete.getDob(),
                                             meetIds.size(), meetDates);
    }


    private void printOrphanReport(PrintWriter pw, List<Orphan> orphans)
    {
        Map<Integer, String> meetIdToDateMap = createMeetIdToDateMap();
        Map<Integer, String> teamIdToNameMap = createTeamIdToNameMap();

        pw.print("Team Name,Meet Date,Meet ID,Athlete,Reason\n");
        for (Orphan orphan : orphans) {
            int    meetId   = orphan.getMeetId();
            String meetDate = meetIdToDateMap.get(meetId);
            String teamName = teamIdToNameMap.get(orphan.getTeamId());

            pw.printf("%s,%s,%d,\"%s\",%n", teamName, meetDate, meetId, orphan.getAthleteInfo());
        }
    }
}
