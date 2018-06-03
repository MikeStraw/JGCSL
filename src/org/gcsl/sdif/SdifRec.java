package org.gcsl.sdif;

// This class reprsents the data from a single line of an SDIF file
public class SdifRec
{
    public enum SdifRecType {
        FILE_DESCRIPTION_REC   ("A0"),
        ROSTER_ONLY_REC        ("A1"),   // Swimtopia roster hy3 file uses this a FILE_DESC_REC
        MEET_REC               ("B1"),
        MEET_HOST_REC          ("B2"),
        TEAM_ID_REC            ("C1"),
        TEAM_ENTRY_REC         ("C2"),
        INDIVIDUAL_EVENT_REC   ("D0"),
        INDIVIDUAL_ADMIN_REC   ("D1"),
        INDIVIDUAL_CONTACT_REC ("D2"),
        INDVIDUAL_INFO_REC     ("D3"),
        SPLIT_REC              ("G0"),
        RELAY_EVENT_REC        ("E0"),
        RELAY_NAME_REC         ("F0"),
        FILE_TERMINATOR_REC    ("Z0"),
        INVALID_REC            ("XX"); // indicates processing error

        private final String code;

        SdifRecType(String codeAbbreviation) {
            code = codeAbbreviation;
        }

        public String getCode() { return code; }

        public static SdifRec.SdifRecType fromString(String code) {
            for (SdifRec.SdifRecType r : SdifRec.SdifRecType.values()) {
                if (r.getCode().equals(code)) {
                    return r;
                }
            }
            return SdifRec.SdifRecType.INVALID_REC;
        }
    };

    private String      dataBuf;   // holds SDIF data
    private SdifRecType type;      // type of SDFI data

    public SdifRec(String sdifData)
    {
        dataBuf = sdifData;
        if (sdifData == null || sdifData.length() < 2) {
            type = SdifRecType.INVALID_REC;
        }
        else {
            type = SdifRecType.fromString(sdifData.substring(0, 2));
        }
    }

    public String getDataBuf()   { return dataBuf; }
    public SdifRecType getType() { return type; }
}
