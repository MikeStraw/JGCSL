package org.gcsl.util;

public class Utils
{
    private Utils() {}

    public final static int INVALID_ID = -1;

    // makeDateString - assumes input is mmddyyyy and returns yyyy-mm-dd which is suitable for SQLite
    public static String makeDateString(String buf) {
        String dateStr;

        if (buf.length() >= 8) {
            dateStr = buf.substring(4, 8) + "-" + buf.substring(0, 2) + "-" +  buf.substring(2, 4);
        }
        else {
            dateStr = "";
        }

        return dateStr;
    }
}
