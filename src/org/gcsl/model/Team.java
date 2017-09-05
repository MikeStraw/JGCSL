package org.gcsl.model;

public class Team
{
    private int    id;
    private String code;
    private String name;
    private String lastUpdate;

//    public Team(String sdifBuf) throws Exception
//    {
//        id = Utils.INVALID_ID;
//        lastUpdate = "";
//        popFromSdifData(sdifBuf);
//    }
    public Team(int id, String shortName, String longName, String lastUpdate)
    {
        this.id         = id;
        this.code       = shortName;
        this.name       = longName;
        this.lastUpdate = lastUpdate;
    }

    // ********** Public Getters
    public int    getId()         { return id; }
    public String getCode()       { return code; }
    public String getName()       { return name; }
    public String getLastUpdate() { return lastUpdate; }

    @Override
    public boolean equals(Object o) {
        if (o == this)              return true;
        if (! (o instanceof Team))  return false;

        // check everything except id and lastUpdate
        Team rhs = (Team) o;
        return code.equals(rhs.code)  &&  name.equals(rhs.name);
    }

    @Override
    public int hashCode() {
        String hashString = makeHash();
        return hashString.hashCode();
    }

    @Override
    public String toString() { return makeHash(); }

    private String makeHash() {
        return code + ":" + name;
    }


    // **********
//    private void popFromSdifData(String sdifBuf) throws Exception
//    {
//        SdifReader.SdifRecType rt  = SdifReader.SdifRecType.fromString(sdifBuf.substring(0,2));
//
//        if (rt == SdifReader.SdifRecType.TEAM_ID_REC) {
//            code = sdifBuf.substring(13, 17).trim();
//            name = sdifBuf.substring(17, 47).trim();
//        }
//
//        if (code.isEmpty()  ||  name.isEmpty() ) {
//            throw new Exception("Invalid Team Data");
//        }
//    }

}
