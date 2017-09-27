package org.gcsl;

import javafx.concurrent.Task;
import org.gcsl.model.MeetResults;

import java.sql.Connection;
import java.util.List;

public class ResultsToDbTask extends Task<Void>
{
    private Connection dbConn;
    private List<MeetResults> meetResults = null;

    ResultsToDbTask(Connection dbConn, List<MeetResults> meetResults)
    {
        this.dbConn = dbConn;
        this.meetResults = meetResults;
    }

    @Override
    protected Void call() throws Exception
    {
        int curItem = 0;
        int numItems = meetResults.size();
        System.out.printf("Inside ResultsToDbTask, meetResults.size()=%d. %n", meetResults.size());

        for (MeetResults meet : meetResults) {
            if (isCancelled()) {
                break;
            }
            curItem++;


            updateMessage("Processing results for meet: " + meet.getName());
            updateProgress(curItem, numItems);
        }

        updateMessage("Successfully processed " + meetResults.size() + " meet results to the DB.");
        return null;
    }
}
