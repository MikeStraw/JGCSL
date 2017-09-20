package org.gcsl.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.gcsl.GcslApp;

public class GcslAppController
{
    private GcslApp app;

    @FXML private Pane imagePane;
    @FXML private Label status;

    @FXML private void handleProcessResults(/* ActionEvent event */)
    {
        System.out.println("Process Results Clicked.");
    }

    @FXML private void handleProcessRosters(/* ActionEvent event */)
    {
        System.out.println("Process Rosters Clicked.");
        app.processRosters();
    }

    @FXML private void handleReportsChampsExceptions(/* ActionEvent event */)
    {
        System.out.println("Reports Champs Exceptions Clicked.");
    }

    @FXML private void handleReportsMeetCount(/* ActionEvent event */)
    {
        System.out.println("Reports Meet Count Clicked.");
    }

    @FXML private void handleReportsOrphans(/* ActionEvent event */)
    {
        System.out.println("Reports Orphans Clicked.");
    }

    @FXML private void initialize()
    {
        status.setText("Opening SQLite DB ...");
    }

    public void setApp(GcslApp app)
    {
        this.app = app;
    }

    public void setStatus(String text)
    {
        status.setText(text);
    }

}
