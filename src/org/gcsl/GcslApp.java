package org.gcsl;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gcsl.model.ProcessArchiveItem;
import org.gcsl.view.ProcessRosterDialogController;
import org.gcsl.view.RootLayoutController;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class GcslApp
{
    private Properties           config;
    private Connection           dbConn;
    private RootLayoutController gcslAppController;
    private Stage                primaryStage;


    // Create the app with a stage connected to the FXML file identified by fxmlPath.
    GcslApp(Stage primaryStage, String fxmlPath) throws Exception
    {
        initRootLayout(primaryStage, fxmlPath);
        loadConfig();
        connectToDb();
    }


    // show the GCSL app UI.
    void start()
    {
        primaryStage.show();
    }


    // ********************     Public Methods
    // Process all the rosters in a particular directory.  This method will create
    // new teams in the DB if necessary and add all new athletes.  If a team with
    // athletes already exists, this method will add new athletes and remove
    // existing athletes that are not in the new roster.
    //
    public void processRosters()
    {
        List<ProcessArchiveItem> rosterFiles = showProcessRosterDialog();
        processRosterFiles(rosterFiles);
    }


    // ********************     Private Methods
    // Connect to the SQLite database and get the version #
    private void connectToDb() throws SQLException
    {
        String dir = "./";
        String dbFile = config.getProperty("db_file");
        String url = "jdbc:sqlite:" + dir + dbFile;

        System.out.println("Connection to SQLite URL: " + url);
        dbConn = DriverManager.getConnection(url);

        gcslAppController.setStatus("Connected to Sqlite DB " + dbFile + ", version " + getDbVersion());
    }


    private String getDbVersion() throws SQLException
    {
        String version;
        try(Statement statement = dbConn.createStatement()) {
            try(ResultSet rs = statement.executeQuery("select sqlite_version();")) {
                version = rs.getString(1);
                System.out.println(version);
            }
        }
        return version;
    }


    // Initialize the GcslApp GUI layout, by loading the FXML file.
    private void initRootLayout(Stage primaryStage, String fxmlPath) throws IOException
    {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(getClass().getResource(fxmlPath));
        BorderPane root = loader.load();
        gcslAppController = loader.getController();
        gcslAppController.setApp(this);  // allow communication from controller back to app

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("GCSL Registrar");
        this.primaryStage.setScene(new Scene(root, 525, 405));
    }


    private void loadConfig() throws IOException
    {
        config = new Properties();
        config.load(new FileInputStream("jgcsl.properties"));
        System.out.println("Read properties file:  db at " + config.getProperty("db_file"));
    }

    // Process a list of roster files.  Teams and athletes identified in the roster files will
    // be added to the DB.
    private void processRosterFiles(List<ProcessArchiveItem> rosterFiles)
    {
        // create the background task
        RosterFileProcessorTask task = new RosterFileProcessorTask(dbConn, rosterFiles);

        // hook up status line on the GUI to the task message
        Label taskMessage = new Label();
        taskMessage.textProperty().addListener((observable, oldValue, newValue) -> { gcslAppController.setStatus(newValue); });
        taskMessage.textProperty().bind(task.messageProperty());

        /*
         * Really should break into 2 tasks ... RosterFileReaderTask that returns List<Team> teams read from the
         * SDIF roster files.  Then run RostersToDbTask which updates the DB.
         *
         * Set up onSuccess handler
         * task.setOnSucceeded(e -> { call RostersToDbTask ....});
         */

        // start the task
        Thread backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    // Show the process roster dialog which allows the user to select which roster files to process.
    private List<ProcessArchiveItem> showProcessRosterDialog()
    {
        String rosterDir = config.getProperty("rosters_dir");
        List<ProcessArchiveItem> rosterFiles = Collections.emptyList();

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(GcslApp.class.getResource("view/ProcessRosterDialog.fxml"));
            VBox page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Roster Dialog");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            ProcessRosterDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setRosterDir(rosterDir);

            dialogStage.showAndWait();
            rosterFiles = controller.getRosterFiles();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return rosterFiles;
    }
}
