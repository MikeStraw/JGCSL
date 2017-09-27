package org.gcsl.model;

import org.gcsl.sdif.SdifRec;
import org.gcsl.util.Utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MeetInfo
{
    private LocalDate date;
    private String    name;

    public MeetInfo(LocalDate date, String name)
    {
        this.date = date;
        this.name = name;
    }

    public static MeetInfo fromSdif(SdifRec sdifRec)
    {
        return popFromSdifData(sdifRec);
    }

    public LocalDate getDate() { return date; }
    public String    getName() { return name; }

    private static MeetInfo popFromSdifData(SdifRec rec)
    {
        String sdifData = rec.getDataBuf();
        String dateStr = sdifData.substring(121, 129).trim();
        LocalDate date = LocalDate.parse(dateStr,
                                         DateTimeFormatter.ofPattern("MMddyyyy"));
        String name = sdifData.substring(11, 41).trim();

        return new MeetInfo(date, name);
    }
}
