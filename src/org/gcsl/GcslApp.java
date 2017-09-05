package org.gcsl;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.gcsl.view.RootLayoutController;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GcslApp
{
    private Properties           config;
    private RootLayoutController gcslAppController;
    private Stage                stage;

    // Create the app with a stage connected to the FXML file identified by fxmlPath.
    GcslApp(Stage primaryStage, String fxmlPath) throws IOException
    {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(getClass().getResource(fxmlPath));
        BorderPane root = loader.load();
        gcslAppController = loader.getController();
        gcslAppController.setApp(this);  // allow communication from controller back to app

        stage = primaryStage;
        stage.setTitle("GCSL Registrar");
        stage.setScene(new Scene(root, 525, 405));

        config = new Properties();
        config.load(new FileInputStream("jgcsl.properties"));
        System.out.println("Read properties file:  db at " + config.getProperty("db_file"));
    }

    // show the GCSL app UI.
    void start()
    {
        stage.show();
    }

    // ********************     Public Methods
    // Process all the rosters in a particular directory.  This method will create
    // new teams in the DB if necessary and add all new athletes.  If a team with
    // athletes already exists, this method will add new athletes and remove
    // existing athletes that are not in the new roster.
    //
    public void processRosters()
    {
        System.out.println("GcslApp::processRosters");
    }



}
