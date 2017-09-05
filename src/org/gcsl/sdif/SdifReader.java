package org.gcsl.sdif;

import java.io.*;
import java.util.List;
import java.util.Vector;

public class SdifReader
{
    private SdifFileDescription fileDescription;
    private long                fileLen;
    private String              filePath;
    private BufferedReader      sdifReader;


    // Creates an SdifReader and reads the first line of the file in order
    // to obtain the file type information.
    // Throws an SdifException if the file identified by sdifFilePath cannot
    // be found or if there is an IO error reading the file.
    public SdifReader(String sdifFilePath) throws SdifException{
        try {
            File sdifFile = new File(sdifFilePath);
            fileLen = sdifFile.length();
            filePath = sdifFilePath;
            sdifReader = new BufferedReader(new FileReader(sdifFilePath));

            // first line of file should be File Description Record
            fileDescription = new SdifFileDescription(sdifReader.readLine());
        } catch (Exception e) {
            close();
            throw new SdifException(e);
        }
    }

    public void close()
    {
        if (sdifReader != null) {
            try {
                sdifReader.close();
                sdifReader = null;
            }
            catch (Exception ex) {
                System.err.println("SDIF file close error ... ignore.");
            }
        }
    }

    public SdifFileDescription getFileDescription() { return fileDescription; }
    public long getFileLen()                        { return fileLen; }
    public String getFilePath()                     { return filePath; }

    // Reads the SdifFile and returns a list of records.
    // Note that the file is closed after the call to readFile
    // Throws an SdifException if there was an IO error reading the file.
    public List<SdifRec> readFile() throws SdifException
    {
        String line;
        Vector<SdifRec> recs = new Vector<>();

        try {
            while ( (line = sdifReader.readLine()) != null) {
                recs.add(new SdifRec(line));
            }
        } catch (IOException e) {
            close();
            throw new SdifException(e);
        }
        close();
        return recs;
    }
}