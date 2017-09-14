package org.gcsl.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils
{
    private Utils() {}

    public final static int INVALID_ID = -1;

    public enum ARCHIVE_FILE_TYPE { ZIP, SD3, UNKNOWN }

    // makeDateString - assumes input is mmddyyyy and returns yyyy-mm-dd which is suitable for SQLite
    public static String makeDateString(String buf)
    {
        String dateStr;

        if (buf.length() >= 8) {
            dateStr = buf.substring(4, 8) + "-" + buf.substring(0, 2) + "-" +  buf.substring(2, 4);
        }
        else {
            dateStr = "";
        }

        return dateStr;
    }


    // Determine the type of the file identified by filePath.  This method is only checking for
    // SD3 and ZIP extensions.
    public static Utils.ARCHIVE_FILE_TYPE getArchiveFileType(String filePath)
    {
        return getArchiveFileType(new File(filePath));
    }

    public static Utils.ARCHIVE_FILE_TYPE getArchiveFileType(File file)
    {
        String fileName = file.getName();
        ARCHIVE_FILE_TYPE type = ARCHIVE_FILE_TYPE.UNKNOWN;

        if (file.exists()  &&  ! file.isDirectory()  &&  fileName.length() > 4) {
            if (fileName.substring(fileName.length()-4).equalsIgnoreCase(".sd3")) {
                type = ARCHIVE_FILE_TYPE.SD3;
            }
            else if (fileName.substring(fileName.length()-4).equalsIgnoreCase(".zip")) {
                type = ARCHIVE_FILE_TYPE.ZIP;
            }
        }

        return type;
    }

    // Extract the file identified by fileName from the archive file.  Return the path
    // to the extracted file.
    public static String getFileFromArchive(String archivePath, String fileName) throws IOException
    {
        String extractedFilePath = "";

        try (ZipFile zipFile = new ZipFile(archivePath)){
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                String zipEntryName = entry.getName();

                if (zipEntryName.equals(fileName)) {
                    extractedFilePath = extractArchiveEntry(entry, zipFile.getInputStream(entry));
                }
            }
        }

        return extractedFilePath;
    }

    public static List<String> getFileNamesFromArchive(String filePath)
    {
        return getFileNamesFromArchive(new File(filePath));
    }

    public static List<String> getFileNamesFromArchive(File file)
    {
        ARCHIVE_FILE_TYPE archiveType = getArchiveFileType(file);
        List<String> files = new ArrayList<>();

        if (archiveType == ARCHIVE_FILE_TYPE.SD3) {
            files.add(file.getName());
        }
        else if (archiveType == ARCHIVE_FILE_TYPE.ZIP) {
            try (ZipFile zipEntryFile = new ZipFile(file.getPath())){
                final Enumeration<? extends ZipEntry> entries = zipEntryFile.entries();

                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    files.add(entry.getName());
                }
            }
            catch (IOException e) {
                // suppress error
                System.err.println("WARNING:  I/O error reading archive: " + e.getMessage());
            }
        }
        return files;
    }

    public static List<File> getFilesFromDirectory(File dir, List<String>extensions)
    {
        List<File> files = new ArrayList<>();
        for (String extension : extensions) {
            files.addAll( getFilesFromDirectory(dir, extension) );
        }
        return files;
    }

    public static List<File> getFilesFromDirectory(File dir, String extension)
    {
        List<File> files = new ArrayList<>();

        if (dir.isDirectory() && extension.length() > 1) {
            FilenameFilter filter = (d, name) -> name.toLowerCase().endsWith(extension.toLowerCase());

            files = Arrays.asList(dir.listFiles(filter));
        }

        return files;
    }


    private static String extractArchiveEntry(final ZipEntry entry, InputStream is) throws IOException
    {
        final int BUFFER_SIZE = 8192;
        String extractedFile = "./temp_" + entry.getName();

        try (FileOutputStream fos = new FileOutputStream(extractedFile)){
            final byte[] buf = new byte[BUFFER_SIZE];
            int length;

            while ((length = is.read(buf, 0, buf.length)) >= 0) {
                fos.write(buf, 0, length);
            }
        }
        return extractedFile;
    }
}
