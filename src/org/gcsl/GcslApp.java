package org.gcsl;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gcsl.model.MeetResults;
import org.gcsl.model.ProcessArchiveItem;
import org.gcsl.model.Team;
import org.gcsl.view.GcslAppController;
import org.gcsl.view.ProcessResultsDialogController;
import org.gcsl.view.ProcessRosterDialogController;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class GcslApp extends Application
{
    private Properties        config;
    private Connection        dbConn;
    private GcslAppController gcslAppController;
    private Stage             primaryStage;


    @Override
    public void start(Stage primaryStage) throws Exception
    {
        this.primaryStage = primaryStage;
        initRootLayout();
        loadConfig();
        connectToDb();

        primaryStage.show();
    }


    // ********************     Public Methods
    // Process the meet results in a particular directory.  This method will update the
    // Athlete-Meet table based on the results.  Orphan table entries may be created
    // for athletes that have results but are not found in a team roster.
    public void processMeetResults()
    {
        List<ProcessArchiveItem> resultFiles = showProcessResultsDialog();
        processResultFiles(resultFiles);
    }


    // Process the rosters in a particular directory.  This method will create
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
    // bind the updateMessage calls from the task to the App's status label
    private Label bindTaskMessageToStatus(Task task)
    {
        Label taskMessageLabel = new Label();
        taskMessageLabel.textProperty().addListener((observable, oldValue, newValue) ->  gcslAppController.setStatus(newValue) );
        taskMessageLabel.textProperty().bind(task.messageProperty());

        return taskMessageLabel;
    }


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
    private void initRootLayout() throws IOException
    {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(getClass().getResource("view/GcslApp.fxml"));
        BorderPane root = loader.load();
        gcslAppController = loader.getController();
        gcslAppController.setApp(this);  // allow communication from controller back to app

        this.primaryStage.setTitle("GCSL Registrar");
        this.primaryStage.setScene(new Scene(root, 525, 405));
    }


    private void loadConfig() throws IOException
    {
        config = new Properties();
        config.load(new FileInputStream("jgcsl.properties"));
        System.out.println("Read properties file:  db at " + config.getProperty("db_file"));
    }


    // The task OnFailure method that puts the tasks' exception message onto the status line.
    private void onTaskFailure(Task task)
    {
        String errMsg = "Task Failure:  " + task.getException();
        System.err.println(errMsg);

        Platform.runLater( () -> gcslAppController.setStatus(errMsg) );
    }

    // The task OnFailure method that puts the tasks' exception message onto the status line
    // and rolls back the DB.
    private void onTaskFailureWithRollback(Task task)
    {
        onTaskFailure(task);
        try {
            dbConn.rollback();
            dbConn.setAutoCommit(true);
        } catch (SQLException e1) {
            System.out.println(e1.getMessage());
        }
    }


    // The task OnSuccess method that kicks off the tasks that adds the meet results to the DB
    private void onTaskSuccess(ReadResultFilesTask task, Label taskMessage)
    {
        List<MeetResults> meetResults = task.getValue();
        ResultsToDbTask dbTask = new ResultsToDbTask(dbConn, meetResults);

        try {
            dbConn.setAutoCommit(false);  // for performance reasons
        } catch (SQLException e) {
            e.printStackTrace();
        }

        dbTask.setOnSucceeded(event -> onTaskSuccessCommit() );
        dbTask.setOnFailed(event -> onTaskFailureWithRollback(dbTask));

        taskMessage.textProperty().unbind();
        taskMessage.textProperty().bind(dbTask.messageProperty());

        startTask(dbTask);
    }


    // The task OnSuccess method that kicks off the task that adds the teams/rosters to the DB.
    private void onTaskSuccess(ReadRosterFilesTask task, Label taskMessage)
    {
        List<Team> teamsFromArchive = task.getValue();
        RostersToDbTask dbTask = new RostersToDbTask(dbConn, teamsFromArchive);

        try {
            dbConn.setAutoCommit(false);  // for performance reasons
        } catch (SQLException e) {
            e.printStackTrace();
        }

        dbTask.setOnSucceeded(event -> onTaskSuccessCommit() );
        dbTask.setOnFailed(event -> onTaskFailureWithRollback(dbTask));

        taskMessage.textProperty().unbind();
        taskMessage.textProperty().bind(dbTask.messageProperty());

        startTask(dbTask);
    }


    private void onTaskSuccessCommit()
    {
        try {
            dbConn.commit();
            dbConn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void processResultFiles(List<ProcessArchiveItem> resultFiles)
    {
        ReadResultFilesTask readResultFilesTask = new ReadResultFilesTask(resultFiles);
        Label taskMessage = bindTaskMessageToStatus(readResultFilesTask);

        readResultFilesTask.setOnSucceeded(event -> onTaskSuccess(readResultFilesTask, taskMessage));
        readResultFilesTask.setOnFailed(event -> onTaskFailure(readResultFilesTask));

        startTask(readResultFilesTask);
    }


    // Process a list of roster files.  Teams and athletes identified in the roster files will
    // be added to the DB.
    private void processRosterFiles(List<ProcessArchiveItem> rosterFiles)
    {
        ReadRosterFilesTask readRosterFilesTask = new ReadRosterFilesTask(rosterFiles);
        Label taskMessage = bindTaskMessageToStatus(readRosterFilesTask);

        readRosterFilesTask.setOnSucceeded(event -> onTaskSuccess(readRosterFilesTask, taskMessage));
        readRosterFilesTask.setOnFailed(event -> onTaskFailure(readRosterFilesTask));

        startTask(readRosterFilesTask);
    }


    // Show the process results dialog which allows the user to select which meet result files to process.
    private List<ProcessArchiveItem> showProcessResultsDialog()
    {
        String resultsDir = config.getProperty("results_dir");
        List<ProcessArchiveItem> resultFiles = Collections.emptyList();

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(GcslApp.class.getResource("view/ProcessResultsDialog.fxml"));
            VBox page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Results Dialog");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            ProcessResultsDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setResultDir(resultsDir);

            dialogStage.showAndWait();
            resultFiles = controller.getResultFiles();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return resultFiles;
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


    private void startTask(Task task)
    {
        Thread backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }
}
