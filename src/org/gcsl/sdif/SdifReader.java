package org.gcsl.sdif;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

public class SdifReader
{
    public enum SdifFileFormat {
        CL2("CL2"),
        HY3("HY3"),
        SD3("SD3"),
        UNKOWN("");

        private final String format;

        SdifFileFormat(String fmt) { this.format = fmt; }
        public String getFormat()  { return format; }

        public static SdifReader.SdifFileFormat fromString(String fmt) {
            for (SdifReader.SdifFileFormat f : SdifReader.SdifFileFormat.values()) {
                if (f.getFormat().equalsIgnoreCase(fmt)) {
                    return f;
                }
            }
            return SdifFileFormat.UNKOWN;
        }
    }

    private SdifFileDescription fileDescription;
    private SdifFileFormat      fileFormat;
    private long                fileLen;
    private String              filePath;
    private BufferedReader      reader;


    // Creates an SdifReader and reads the first line of the file in order
    // to obtain the file type information.
    // Throws an SdifException if the file identified by sdifFilePath cannot
    // be found or if there is an IO error reading the file.
    public SdifReader(String sdifFilePath) throws SdifException{
        try {
            File sdifFile = new File(sdifFilePath);
            fileFormat    = SdifFileFormat.fromString(getFileExtension(sdifFile));
            fileLen       = sdifFile.length();
            filePath      = sdifFilePath;
            reader        = new BufferedReader(new FileReader(sdifFilePath));

            // first line of file should be File Description Record
            fileDescription = new SdifFileDescription(reader.readLine());
        } catch (Exception e) {
            close();
            throw new SdifException(e);
        }
    }

    public void close()
    {
        if (reader != null) {
            try {
                reader.close();
            }
            catch (Exception ex) {
                System.err.println("SDIF file close error ... ignore.");
            }
            reader = null;
        }
    }

    public SdifFileDescription getFileDescription() { return fileDescription; }
    public SdifFileFormat getFileFormat()           { return fileFormat; }
    public long getFileLen()                        { return fileLen; }
    public String getFilePath()                     { return filePath; }


    // Reads the SdifFile and returns a list of records.
    // Note that the file is closed after the call to readFile
    // Throws an SdifException if there was an IO error reading the file.
    public List<SdifRec> readFile() throws SdifException
    {
        String line;
        ArrayList<SdifRec> recs = new ArrayList<>();

        try {
            while ( (line = reader.readLine()) != null) {
                recs.add(new SdifRec(line));
            }
        } catch (IOException e) {
            close();
            throw new SdifException(e);
        }
        close();
        return recs;
    }


    // Get the extension portion of the file name
    private String getFileExtension(File f) {
        String extension = "";
        String fileName = f.getName();

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1);
        }
        return extension;
    }
}