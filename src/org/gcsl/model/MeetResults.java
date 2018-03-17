package org.gcsl.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MeetResults
{
    private MeetInfo info;
    private String resultsFileDate;
    private List<Athlete> orphans = new ArrayList<>();
    private ProcessArchiveItem.Scenario scenario;
    private List<Team> teams = new ArrayList<>();

    public MeetResults(MeetInfo meetInfo, ProcessArchiveItem.Scenario resultsType)
    {
        info = meetInfo;
        scenario = resultsType;
    }
    public MeetResults(LocalDate meetDate, String meetName, ProcessArchiveItem.Scenario resultsType)
    {
        this(new MeetInfo(meetDate, meetName), resultsType);

    }

    public void addOrphan(Athlete orphan)         { orphans.add(orphan); }
    public void addOrphans(List<Athlete> orphans) { this.orphans.addAll(orphans); }
    public void addTeam(Team team)                { teams.add(team); }

    public LocalDate getDate()             { return info.getDate(); }
    public String getName()                { return info.getName(); }
    public List<Athlete> getOrphans()      { return Collections.unmodifiableList(orphans); }
    public String getResultsFileDate()     { return resultsFileDate; }
    public ProcessArchiveItem.Scenario
                  getResultsScenario()     { return scenario; }
    public List<Team> getTeams()           { return teams; }

    public void setResultFileDate(String date){ resultsFileDate = date; }
}
