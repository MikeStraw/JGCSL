package org.gcsl.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Vector;

public class MeetResults
{
    MeetInfo info;
    ProcessArchiveItem.Scenario scenario;
    List<Team> teams = new Vector<>();

    public MeetResults(MeetInfo meetInfo, ProcessArchiveItem.Scenario resultsType)
    {
        info = meetInfo;
        scenario = resultsType;
    }
    public MeetResults(LocalDate meetDate, String meetName, ProcessArchiveItem.Scenario resultsType)
    {
        this(new MeetInfo(meetDate, meetName), resultsType);

    }

    public void addTeam(Team team) { teams.add(team); }
    public boolean isByWeek()      { return scenario == ProcessArchiveItem.Scenario.BYE_WEEK_RESULTS; }
    public boolean isRainOut()     { return scenario == ProcessArchiveItem.Scenario.RAIN_OUT_RESULTS; }
    public LocalDate getDate()     { return info.getDate(); }
    public String getName()        { return info.getName(); }
    public List<Team> getTeams()   { return teams; }


}
