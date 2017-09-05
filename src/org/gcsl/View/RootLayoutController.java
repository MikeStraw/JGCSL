package org.gcsl.View;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.gcsl.GcslApp;

public class RootLayoutController
{
    // TODO:  HOw to make image resize properly.  BorderPane has issues with not clipping content.
    //        Possibly try AnchorPane with VBox -OR- nest HBox inside center of BorderPane.
    private GcslApp app;

    @FXML private ImageView imageView;
    @FXML private Label status;

    @FXML private void handleProcessResults(ActionEvent event)
    {
        System.out.println("Process Results Clicked.");
    }

    @FXML private void handleProcessRosters(ActionEvent event)
    {
        System.out.println("Process Rosters Clicked.");
    }

    @FXML private void handleReportsChampsExceptions(ActionEvent event)
    {
        System.out.println("Reports Champs Exceptions Clicked.");
    }

    @FXML private void handleReportsMeetCount(ActionEvent event)
    {
        System.out.println("Reports Meet Count Clicked.");
    }

    @FXML private void handleReportsOrphans(ActionEvent event)
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
}
