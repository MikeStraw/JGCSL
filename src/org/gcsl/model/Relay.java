package org.gcsl.model;


import org.gcsl.sdif.SdifRec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Relay
{
    private List<Athlete> athletes = new ArrayList<>();
    private String        name;
    private boolean       noShowResult;

    public Relay(String name)
    {
        this.name = name;
        this.noShowResult = false;
    }
    public Relay(String name, boolean noShow)
    {
        this.name = name;
        this.noShowResult = noShow;
    }

    public void addAthlete(Athlete athlete) { athletes.add(athlete); }
    public List<Athlete> getAthletes()      { return Collections.unmodifiableList(athletes); }
    public String getName()                 { return name; }
    public boolean isNoShow()               { return noShowResult; }

    // Factory method to create a Team object from SDIF data
    public static Relay fromSdifData(SdifRec rec)
    {
        String sdifData  = rec.getDataBuf();
        String finalTime = sdifData.substring(72, 80).trim();
        boolean noShow   = finalTime.equals("NS");
        String relayName = sdifData.substring(11, 12);         // 'A', 'B', 'C', etc.
        String teamCode  = sdifData.substring(12, 18).trim();

        return  new Relay(teamCode + "-" + relayName, noShow);
    }
}
