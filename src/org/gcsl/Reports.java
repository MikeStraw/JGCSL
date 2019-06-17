package org.gcsl;

import org.gcsl.db.AthleteDbo;
import org.gcsl.db.MeetDbo;
import org.gcsl.db.OrphanDbo;
import org.gcsl.db.TeamDbo;
import org.gcsl.model.*;
import org.gcsl.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

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

    // Report the Champs exceptions
    //   1.  Divers not on a roster (dive orphans)
    //   2.  Divers not in the TM entry file
    //   3.  Athletes from the TM entry file not on the roster (orphans)
    //   4.  Athletes not meeting the 3 meet requirement
    public void champsExceptionReport(List<Team> teams,
                                      Map<Integer, TeamDiveEntries> diveEntriesMap) throws IOException, SQLException
    {
        String fileSpec = Utils.createFileSpec(reportDir, "champs_exceptions", "txt", false);
        String trailerText = "================================================\n";

        try (PrintWriter pw = new PrintWriter(fileSpec)) {
            for(Team team : teams) {
                String headerText = "Championship exceptions for: " + team.getName() + ", size=" + team.getTeamSize();
                System.out.println(headerText);

                pw.println(headerText);
                printDiveOrphans(pw, diveEntriesMap.get(team.getId()));
                printDiveEntryExceptions(pw, team, diveEntriesMap.get(team.getId()));
                printEntryExceptions(pw, team);
                pw.println(trailerText);

                System.out.println(trailerText);
            }
        }
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


    private void printDiveEntryExceptions(PrintWriter pw, Team team, TeamDiveEntries diveEntries)
    {
        int numDiveEntries = diveEntries != null ? diveEntries.getAthletes().size() : 0;
        int numDiveExceptions = 0;

        pw.print("\tDive Exceptions: ");
        if (numDiveEntries != 0) {
            // walk through the dive athletes and make sure they are on the team's
            // entry roster (ie, make sure they were included in the TM entry file).
            Set<Athlete> teamEntries = team.getAthletes();
            for(Athlete diveAthlete : diveEntries.getAthletes()) {
                diveAthlete.setTeamId(team.getId());
                if (! teamEntries.contains(diveAthlete)) {
                    numDiveExceptions++;
                    pw.printf("%n \t\t%s,%s,%s", diveAthlete.getName(), diveAthlete.getGender(), diveAthlete.getDob());
                }
            }
        }
        if (numDiveExceptions == 0)  pw.print("NONE");
        pw.println();

        System.out.println("\tdive exceptions: " + numDiveExceptions);
    }


    private void printDiveOrphans(PrintWriter pw, TeamDiveEntries diveEntries)
    {
        int numOrphans = diveEntries != null ? diveEntries.getOrphans().size() : 0;

        pw.print("\tDive Orphans: ");
        if (numOrphans == 0) {
            pw.println("NONE");
        }
        else {
            diveEntries.getOrphans().forEach(orphan -> pw.printf("\t\t%s %n", orphan));
        }

        System.out.println("\tdive orphans: " + numOrphans);
    }


    private String entryExceptionToString(Athlete athlete, List<Integer> meetIds)
    {
        Map<Integer, String> meetIdToDateMap = createMeetIdToDateMap();
        String meetDates = meetIdsToDateString(meetIds, meetIdToDateMap);
        StringBuilder sb = new StringBuilder();

        sb.append("\t\t")
          .append(athlete.getName()).append(",")
          .append(athlete.getGender()).append(",")
          .append(athlete.getDob()).append(",")
          .append("count=").append(meetIds.size()).append(",")
          .append(meetDates);

        return sb.toString();
    }

    private void printEntryExceptions(PrintWriter pw, Team team) throws SQLException
    {
        List<String> exceptionList = new ArrayList<>();
        List<String> orphanList = new ArrayList<>();

        // make sure each entry is found in the DB (ie, not an orphan)
        // make sure each entry has participated in 3 meets
        for (Athlete athlete : team.getAthletes()) {
            Athlete dbAthlete = AthleteDbo.find(db, athlete);

            if (dbAthlete == null) {
                orphanList.add(entryExceptionToString(athlete, Collections.emptyList()));
            }
            else {
                List<Integer> meetIds = AthleteDbo.getMeetIds(db, dbAthlete);
                if (meetIds.size() < 3) {
                    exceptionList.add(entryExceptionToString(dbAthlete, meetIds));
                }
            }
        }

        pw.print("\tChamps orphans: ");
        if (orphanList.size() == 0) {
            pw.println("NONE");
        }
        else {
            pw.println();
            orphanList.forEach(orphan -> pw.println(orphan));
        }

        pw.print("\tChamps Exceptions: ");
        if (exceptionList.size() == 0) {
            pw.println("NONE");
        }
        else {
            pw.println();
            exceptionList.forEach(athlete -> pw.println(athlete));
        }

        System.out.println("\torphans: " + orphanList.size());
        System.out.println("\texceptions: " + exceptionList.size());
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
