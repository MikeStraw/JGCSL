package org.gcsl.sdif;

import org.gcsl.util.Utils;

public class SdifFileDescription
{
    public enum SdifFileType {
        MEET_REGISTRATION    ("01"),   // SD3 roster files have this code
        MEET_RESULTS         ("02"),
        OVC                  ("03"),
        NAT_AGE_GROUP_RECORD ("04"),
        LSC_AGE_GROUP_RECORD ("05"),
        LSC_MOTIVATIONAL_LIST("06"),
        NAT_RECS_AND_RANKINGS("07"),
        TEAM_SELECTION       ("08"),
        LSC_BEST_TIMES       ("09"),
        USS_REGISTRATION     ("10"),
        TOP_16               ("16"),
        VENDOR_DEFINED       ("20"),   // ZIP roster files have this code
        UNKNOWN              ("99");

        private final String type;

        SdifFileType(String type) {
            this.type = type;
        }

        public String getType() { return type; }

        public static SdifFileDescription.SdifFileType fromString(String type) {
            for (SdifFileDescription.SdifFileType t : SdifFileDescription.SdifFileType.values()) {
                if (t.getType().equals(type)) {
                    return t;
                }
            }
            return SdifFileType.UNKNOWN;
        }
    };

    String       fileDate;
    SdifFileType fileType = SdifFileType.UNKNOWN;
    String       vendor;
    String       vendorVersion;

    // Create an SDIF File Descriptor from a File Description Record
    public SdifFileDescription(String sdifBuf) throws SdifException {
        popFromSdifData(sdifBuf);
    }

    // Getters
    public String       getFileDate()      { return fileDate; }
    public SdifFileType getFileType()      { return fileType; }
    public String       getVendor()        { return vendor; }
    public String       getVendorVersion() { return vendorVersion; }


    private void popFromSdifData(String sdifBuf) throws SdifException
    {
        if (sdifBuf.length() < 2) {
            throw new SdifException("Invalid File Description data:  not enough data");
        }

        SdifRec.SdifRecType rt = SdifRec.SdifRecType.fromString(sdifBuf.substring(0,2));
        if (rt != SdifRec.SdifRecType.FILE_DESCRIPTION_REC) {
            throw new SdifException("Invalid File Description data:  not file description record");
        }

        fileDate = Utils.makeDateString(sdifBuf.substring(105, 113));
        fileType = SdifFileType.fromString(sdifBuf.substring(11,13));
        vendor   = sdifBuf.substring(43, 63).trim();
        vendorVersion = sdifBuf.substring(63, 73).trim();
    }
}
