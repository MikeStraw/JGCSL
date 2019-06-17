package org.gcsl.model;

import org.gcsl.sdif.SdifRec;
import org.gcsl.util.Utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Team
{
    private int    id;
    private String code;
    private String name;
    private String lastUpdate;
    private Set<Athlete> athletes = new HashSet<>();


    // CTOR used when reading data from DB or when all the components are known
    public Team(int id, String shortName, String longName, String lastUpdate)
    {
        this.id         = id;
        this.code       = shortName;
        this.name       = longName;
        this.lastUpdate = lastUpdate;
    }

    // Factory method to create a Team object from SDIF data
    public static Team fromHy3Data(SdifRec rec)
    {
        String sdifData = rec.getDataBuf();
        String code = sdifData.substring(2, 7).trim();
        String name = sdifData.substring(7, 37).trim();

        return new Team(Utils.INVALID_ID, code, name, "");
    }
    public static Team fromSdifData(SdifRec rec)
    {
        String sdifData = rec.getDataBuf();
        String code = sdifData.substring(13, 17).trim();
        String name = sdifData.substring(17, 47).trim();

        return new Team(Utils.INVALID_ID, code, name, "");
    }


    public void addAthlete(Athlete athlete)
    {
        athlete.setTeamId(id);
        athletes.add(athlete);
    }

    public void removeAthlete(Athlete athlete)
    {
        athletes.remove(athlete);
    }

    // Add all the athletes from the set of newAthletes.  It is assumed that all the
    // new athletes already have the teamId value set properly.
    public void addRoster(Set<Athlete> newAthletes)
    {
        athletes.addAll(newAthletes);
    }

    // ********** Public Getters
    public Set<Athlete> getAthletes()  { return Collections.unmodifiableSet(athletes); }

    public int    getId()         { return id; }
    public String getCode()       { return code; }
    public String getName()       { return name; }
    public String getLastUpdate() { return lastUpdate; }
    public int    getTeamSize()   { return athletes.size(); }

    public void setId(int newId)
    {
        id = newId;

        // It's a "no-no" to update items in a Hash, so we must create a new one
        Set<Athlete> newSet = new HashSet<>();
        athletes.forEach(a -> {
            a.setTeamId(id);
            newSet.add(a);
        } );
        this.athletes = newSet;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)              return true;
        if (! (o instanceof Team))  return false;

        // check everything except lastUpdate
        Team rhs = (Team) o;
        return id == rhs.id  &&  code.equals(rhs.code)  &&  name.equals(rhs.name);
    }

    @Override
    public int hashCode() {
        String hashString = makeHash();
        return hashString.hashCode();
    }

    @Override
    public String toString() { return makeHash(); }

    private String makeHash() {
        return Integer.toString(id) +":" + code + ":" + name;
    }
}
