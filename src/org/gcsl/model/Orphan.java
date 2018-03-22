package org.gcsl.model;

public class Orphan
{
    private Athlete athlete;
    private int     meetId;

    public Orphan(int meetId, String name, String gender, String dob, int teamId)
    {
        this.meetId = meetId;
        this.athlete = new Athlete(name, gender, dob, teamId);
    }

    public String getAthleteInfo() { return createAthleteInfo(); }
    public int    getMeetId()      { return meetId; }
    public int    getTeamId()      { return athlete.getTeamId(); }

    private String createAthleteInfo()
    {
        return athlete.getName()   + " - "  +
               athlete.getGender() + ",  "  +
               athlete.getDob();
    }

}
