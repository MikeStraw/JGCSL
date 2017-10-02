package org.gcsl.model;

import org.gcsl.sdif.SdifException;
import org.gcsl.sdif.SdifRec;
import org.gcsl.util.Utils;

public class Athlete
{
    private String dob;                          // date of birth ('yyyy-mm-dd')
    private String gender;                       // 'M' or 'F'
    private int    id = Utils.INVALID_ID;        // athlete ID in DB
    private String lastUpdate = "";              // database update timestamp
    private String name;                         // Athlete's name (last, first [m])
    private int    teamId = Utils.INVALID_ID;    // ID of team athlete belongs to

    // Create from individual components
    public Athlete (String name, String gender, String dob)
    {
        this.name = name;
        this.gender = gender;
        this.dob = dob;
    }
    public Athlete (String name, String gender, String dob, int teamId)
    {
        this(name, gender, dob);
        this.teamId = teamId;
    }
    public Athlete(int id, String name, String gender, String dob, int teamId, String lastUpdate)
    {
        this(name, gender, dob, teamId);
        this.id = id;
        this.lastUpdate = lastUpdate;
    }

    public static Athlete fromSdif(SdifRec sdifRec) throws SdifException
    {
        return popFromSdifData(sdifRec);
    }

    // ********** Public Getters
    public String getDob()        { return dob; }
    public String getGender()     { return gender; }
    public int    getId()         { return id; }
    public String getLastUpdate() { return lastUpdate; }
    public String getName()       { return name; }
    public int    getTeamId()     { return teamId; }

    // ********** Public Setters
    public void setId(int id)         { this.id = id; }
    public void setTeamId(int teamId) { this.teamId = teamId; }

    @Override
    public boolean equals(Object o) {
        if (o == this)                 return true;
        if (! (o instanceof Athlete))  return false;

        // check everything except id and lastUpdate
        Athlete rhs = (Athlete) o;
        return dob.equals(rhs.dob)  &&  gender.equals(rhs.gender)  &&  name.equals(rhs.name)  &&  teamId == rhs.teamId;
    }

    @Override
    public int hashCode() {
        String hashString = makeHash();
        return hashString.hashCode();
    }

    @Override
    public String toString() { return makeHash(); }

    private String makeHash() {
        return name + ":" + dob + ":" + gender + ":" + Integer.toString(teamId);
    }


    private static Athlete popFromSdifData(SdifRec rec) throws SdifException
    {
        int dobIdx = -1, genderIdx = -1, nameIdx = -1;
        int dobLen = 8;
        int genderLen = 1;
        int nameLen = 28;

        // For the different type of athlete name records, set the offset index
        if (rec.getType() == SdifRec.SdifRecType.INDIVIDUAL_EVENT_REC) {
            dobIdx = 55;
            genderIdx=65;
            nameIdx = 11;
        }
        else if (rec.getType() == SdifRec.SdifRecType.INDIVIDUAL_ADMIN_REC) {
            dobIdx = 63;
            genderIdx=73;
            nameIdx = 18;
        }
        else if (rec.getType() == SdifRec.SdifRecType.RELAY_NAME_REC) {
            dobIdx = 65;
            genderIdx=75;
            nameIdx = 22;
        }

        // If the offset index was set, grab the data from the buffer.
        if (nameIdx == -1) {
            throw new SdifException("Invalid SDIF record type: " + rec.getType());
        }
        else {
            String dob=null, gender=null, name=null;

            String sdifData = rec.getDataBuf();
            dob    = Utils.makeDateString(sdifData.substring(dobIdx, dobIdx+dobLen));
            gender = sdifData.substring(genderIdx, genderIdx+genderLen);
            name   = sdifData.substring(nameIdx, nameIdx+nameLen).trim().replaceAll(" +", " ");

            if (name.length() < 3 || dob.length() != 10 || gender.length() != 1) {
                throw new SdifException("Invalid Athlete Data");
            }

            return new Athlete(name, gender, dob);
        }
    }
}
