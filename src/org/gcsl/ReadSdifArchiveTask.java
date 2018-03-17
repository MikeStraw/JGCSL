package org.gcsl;

import javafx.concurrent.Task;
import org.gcsl.model.ProcessArchiveItem;
import org.gcsl.sdif.SdifException;
import org.gcsl.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

// Abstract class for reading a list of SDIF archive files (ZIP or SD3) and producing
// a set of result files (List<T>) in a separate task.
// When deriving from this class, the processArchiveItem method must be implemented.
public abstract class ReadSdifArchiveTask<T> extends Task<List<T>>
{
    private List<ProcessArchiveItem> archiveItems;

    ReadSdifArchiveTask(List<ProcessArchiveItem> archiveItems)
    {
        this.archiveItems = archiveItems;
    }


    @Override
    protected List<T> call() throws Exception
    {
        int     curItem  = 0;
        int     numItems = archiveItems.size();
        List<T> results  = new ArrayList<>();
        System.out.printf("Inside ReadSdifArchiveTask::call(). archiveItems.size()=%d %n", archiveItems.size());

        for (ProcessArchiveItem archiveItem : archiveItems) {
            if (isCancelled())  { break; }
            curItem++;

            updateMessage("Processing archive: " + archiveItem.getName());
            updateProgress(curItem, numItems);

            T result = processArchiveItem(archiveItem);
            results.add(result);
        }
        updateMessage("Archive files read successfully.");

        return results;
    }


    // Extracts the SDIF file from the archive based the priority ordering of extensions (CL2, HY3, SD3)
    // Return the path to the extracted archive file.
    protected String extractSdifFileFromArchive(ProcessArchiveItem archiveItem) throws IOException, SdifException
    {
        String []archiveContents = orderResultFiles(archiveItem.getContents());
        String   archiveFilePath = archiveItem.getDirectory() + File.separator + archiveItem.getName();
        String   resultFilePath;

        // Extract the results file from the archive
        Utils.ARCHIVE_FILE_TYPE fileType = Utils.getArchiveFileType(archiveFilePath);
        switch (fileType) {
            case ZIP : resultFilePath = Utils.getFileFromArchive(archiveFilePath, archiveContents[0]);
                break;
            case SD3:  resultFilePath = archiveFilePath;
                break;
            default:   throw new SdifException("Unknown roster file archive.  Filepath=" + archiveFilePath);
        }

        return resultFilePath;
    }


    // Order the archive content based on whether to prioritize .CL2 files over .HY3 files.
    private String [] orderResultFiles(String archiveContents)
    {
        // TODO: really should check a config entry, for now just assume that we want .CL2 entries before .HY3
        return archiveContents.split(", ");
    }


    abstract T processArchiveItem(ProcessArchiveItem item) throws SdifException, IOException;
}
