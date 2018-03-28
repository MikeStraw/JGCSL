package org.gcsl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamDiveEntries
{
    private int teamId;
    private List<Athlete> divers;
    private List<String>  orphans;

    public TeamDiveEntries(int teamId)
    {
        this.teamId  = teamId;
        this.divers  = new ArrayList<>();
        this.orphans = new ArrayList<>();
    }

    public void addDiver(Athlete athlete)  { divers.add(athlete); }
    public void addOrphan(String athlete)  { orphans.add(athlete); }

    public List<Athlete> getAthletes()  { return Collections.unmodifiableList(divers); }
    public List<String>  getOrphans()   { return Collections.unmodifiableList(orphans); }
}
