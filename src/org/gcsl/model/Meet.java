package org.gcsl.model;

public class Meet
{
    String fileDate;
    int    id;
    String lastUpdate;
    String meetDate;
    String resultType;
    int    team1Id;
    int    team2Id;

    public Meet(int meetId, int team1, int team2, String dateOfMeet, String type, String sdifFileDate, String lastDbUpdate)
    {
        fileDate   = sdifFileDate;
        id         = meetId;
        lastUpdate = lastDbUpdate;
        meetDate   = dateOfMeet;
        resultType = type;
        team1Id    = team1;
        team2Id    = team2;
    }


    public String getFileDate()   { return fileDate; }
    public int    getId()         { return id; }
    public String getLastUpdate() { return lastUpdate; }
    public String getMeetDate()   { return meetDate; }
    public String getResultType() { return resultType; }
    public int    getTeam1Id()    { return team1Id; }
    public int    getTeam2Id()    { return team2Id; }
}
