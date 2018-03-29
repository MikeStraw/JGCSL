package org.gcsl;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gcsl.db.MeetDbo;
import org.gcsl.db.TeamDbo;
import org.gcsl.model.*;
import org.gcsl.view.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class GcslApp extends Application
{
    private Properties        config;
    private Connection        dbConn;
    private GcslAppController gcslAppController;
    private Stage             primaryStage;

    private class ChampsReportingInfo
    {
        public String entriesDir;
        public String reportsDir;
        public Map<Integer, TeamDiveEntries> diveEntriesMap;

        public ChampsReportingInfo(String entriesDir, String reportsDir, Map<Integer, TeamDiveEntries> diveEntryMap)
        {
            this.entriesDir = entriesDir;
            this.reportsDir = reportsDir;
            this.diveEntriesMap = diveEntryMap;
        }
    }


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
        if (resultFiles.size() > 0) {
            processResultFiles(resultFiles);
        }
    }


    // Process the rosters in a particular directory.  This method will create
    // new teams in the DB if necessary and add all new athletes.  If a team with
    // athletes already exists, this method will add new athletes and remove
    // existing athletes that are not in the new roster.
    //
    public void processRosters()
    {
        List<ProcessArchiveItem> rosterFiles = showProcessRosterDialog();
        if (rosterFiles.size() > 0 ) {
            processRosterFiles(rosterFiles);
        }
    }


    // Report the championship entry exceptions
    public void reportChampsExceptions()
    {
        ChampsReportingInfo champsInfo = showChampsExceptionDialog();
        if (champsInfo == null) {
            gcslAppController.setStatus("Champs Exception Report cancelled.");
        }
        else {
            List<ProcessArchiveItem> entryFiles = showChampEntriesDialog(champsInfo.entriesDir);
            runChampsReportTask(champsInfo, entryFiles);
        }
    }


    // Report the number of meets per athlete
    public void reportMeetCount()
    {
        File reportsDir = getReportsDirectory(config.getProperty("reports_dir"));
        if (reportsDir == null) {
            gcslAppController.setStatus("Report Meet Count cancelled.");
        }
        else {
            Reports reports = new Reports(dbConn, reportsDir);
            try {
                reports.meetCountReport();
                gcslAppController.setStatus("Meet count report created successfully.");
            }
            catch (Exception ex) {
                System.err.println("Caught error creating meet count report file: " + ex.getMessage());
                gcslAppController.setStatus("Error creating Meet Count Report ... ");
            }
        }
    }

    // Report the orphans found in the orphan table
    public void reportOrphans()
    {
        File reportsDir = getReportsDirectory(config.getProperty("reports_dir"));
        if (reportsDir == null) {
            gcslAppController.setStatus("Report Orphans cancelled.");
        }
        else {
            Reports reports = new Reports(dbConn, reportsDir);
            try {
                reports.orphanReport();
                gcslAppController.setStatus("Orphan report created successfully.");
            }
            catch (IOException ex) {
                System.err.println("Caught error creating orphan report file: " + ex.getMessage());
                gcslAppController.setStatus("Error creating Orphan Report ... ");
            }

        }
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


    // Check the meet results to see if any of the meets exist already.
    // Return true if
    //                 1. none of the meets already exist
    //                 2. it is OK to update the existing meet results
    // of false if the new meet results should not be procesed or there is an SQL error.
    private boolean checkExistingResults(List<MeetResults> meetResults)
    {
        List<Integer> existingMeetsIDs = new ArrayList<>();
        boolean rc = false;

        try {
            for (MeetResults meet : meetResults) {
                Meet dbMeet = MeetDbo.findByTeams(dbConn, meet);
                if (dbMeet != null) {
                    existingMeetsIDs.add(dbMeet.getId());
                }
            }
            if (existingMeetsIDs.size() == 0) {
                rc = true;
            }
            else {
                String message = "The following meets already exist: " + existingMeetsIDs.toString();
                rc = showMeetExistsDialog(message);
            }
        } catch (SQLException e) {
            System.err.println("SQL Error checking for meet existence: " + e.getMessage());
        }

        return rc;
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


    private Stage createDialogStage(VBox page, String title)
    {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(primaryStage);
        Scene scene = new Scene(page);
        dialogStage.setScene(scene);

        return dialogStage;
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


    private File getReportsDirectory(String initialDir)
    {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File(initialDir));

        return dc.showDialog(primaryStage);
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


    // Load the configuration information.
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


    // Generic DB task success method that commits the DB and turns auto-commit back on.
    private void onTaskSuccessCommit()
    {
        try {
            dbConn.commit();
            dbConn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Go through the list of incoming MeetResults and "pair up" rain out
    // result entries.  The "paired up" results will be collapsed into a
    // single MeetResult with both teams.
    private List<MeetResults> pairRainOutEntries(List<MeetResults> inResults)
    {
        long numRainouts = inResults.stream()
                                    .filter( meet -> meet.getResultsScenario() == ProcessArchiveItem.Scenario.RAIN_OUT_ENTRIES)
                                    .count();
        System.out.println("Number of rainout entries is: " + numRainouts);

        List<MeetResults> outResults;
        if (numRainouts == 0) {
            outResults = inResults;
        }
        else {
            outResults = showRainOutDialog(inResults);
        }

        return outResults;
    }


    // Process a list of meet result files.  Meets and athlete results identified
    // in the meet result files will be added to the DB.
    private void processResultFiles(List<ProcessArchiveItem> resultFiles)
    {
        ReadResultFilesTask readResultFilesTask = new ReadResultFilesTask(resultFiles);
        Label taskMessage = bindTaskMessageToStatus(readResultFilesTask);

        readResultFilesTask.setOnSucceeded(event -> runResultsToDbTask(readResultFilesTask, taskMessage));
        readResultFilesTask.setOnFailed(event -> onTaskFailure(readResultFilesTask));

        startTask(readResultFilesTask);
    }


    // Process a list of roster files.  Teams and athletes identified in the roster files will
    // be added to the DB.
    private void processRosterFiles(List<ProcessArchiveItem> rosterFiles)
    {
        ReadRosterFilesTask readRosterFilesTask = new ReadRosterFilesTask(rosterFiles);
        Label taskMessage = bindTaskMessageToStatus(readRosterFilesTask);

        readRosterFilesTask.setOnSucceeded(event -> runRostersToDbTask(readRosterFilesTask, taskMessage));
        readRosterFilesTask.setOnFailed(event -> onTaskFailure(readRosterFilesTask));

        startTask(readRosterFilesTask);
    }


    private void runChampsExceptionReport(ReadEntryFilesTask entryFilesTask, ChampsReportingInfo reportingInfo)
    {
        List<Team> teams = new ArrayList<>();
        Reports reports = new Reports(dbConn, new File(reportingInfo.reportsDir));

        // Need to catch exceptions here because the task's onSuccess() method can't
        // deal with exceptions.  (Produces compile error.)
        try {
            // Teams info read from entry files does not have team ID,
            // and consequently, none of the roster athletes do either.
            // So fix that!
            for (Team team : entryFilesTask.getValue()) {
                Team dbTeam = TeamDbo.findByCode(dbConn, team.getCode());
                if (dbTeam == null) {
                    throw new Exception("Team: " + team.getCode() + " not found in the database.");
                }
                team.setId(dbTeam.getId());
                teams.add(team);
            }
            reports.champsExceptionReport(teams, reportingInfo.diveEntriesMap);
        }
        catch (Exception ex) {
            System.err.println("Caught error creating champs exception report file: " + ex.getMessage());
            gcslAppController.setStatus("Error creating Champs Exception Report ... ");
        }
    }


    // Run the champs exception report against a set of champs entry files.
    private void runChampsReportTask(ChampsReportingInfo champsReportInfo,
                                     List<ProcessArchiveItem> entryFiles)
    {
        if (entryFiles.size() == 0) {
            gcslAppController.setStatus("Champs Exception Report ... no entries selected.");
        }
        ReadEntryFilesTask readFilesTask = new ReadEntryFilesTask(entryFiles);
        Label taskMessage = bindTaskMessageToStatus(readFilesTask);

        readFilesTask.setOnSucceeded(event -> {
            taskMessage.textProperty().unbind();
            runChampsExceptionReport(readFilesTask, champsReportInfo);
        });
        readFilesTask.setOnFailed(event -> onTaskFailure(readFilesTask));

        startTask(readFilesTask);
    }


    // Run a task to add the meet results to the DB.
    private void runResultsToDbTask(ReadResultFilesTask task, Label taskMessage)
    {
        List<MeetResults> meetResults = task.getValue();

        // fix up the meet results so that rain out entries are combined
        // into a single MeetResults entry
        meetResults = pairRainOutEntries(meetResults);

        if (! checkExistingResults(meetResults)) {
            gcslAppController.setStatus("Process Meet Results cancelled.");
        }
        else {
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
    }


    // The task OnSuccess method that kicks off the task that adds the teams/rosters to the DB.
    private void runRostersToDbTask(ReadRosterFilesTask task, Label taskMessage)
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


    private List<ProcessArchiveItem> showChampEntriesDialog(String entriesDir)
    {
        List<ProcessArchiveItem> entryFiles = Collections.emptyList();

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(GcslApp.class.getResource("view/ProcessChampEntriesDialog.fxml"));
            VBox page = loader.load();
            Stage dialogStage = createDialogStage(page, "Champs Entries Dialog");

            ProcessChampEntriesDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setEntryDir(entriesDir);

            dialogStage.showAndWait();
            if (! controller.dialogCancelled()) {
                entryFiles = controller.getEntryFiles();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return entryFiles;
    }


    private ChampsReportingInfo showChampsExceptionDialog()
    {
        ChampsReportingInfo champsInfo = null;
        String entriesDir = config.getProperty("entries_dir");
        String reportsDir = config.getProperty("reports_dir");

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(GcslApp.class.getResource("view/ChampsExceptionsDialog.fxml"));
            VBox page = loader.load();
            Stage dialogStage = createDialogStage(page, "Champs Exception Dialog");

            ChampsExceptionsDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setDbConnection(dbConn);
            controller.setEntriesDir(entriesDir);
            controller.setReportsDir(reportsDir);

            dialogStage.showAndWait();
            if (! controller.dialogCancelled()) {
                champsInfo = new ChampsReportingInfo(controller.getEntriesDir(),
                                                     controller.getReportsDir(),
                                                     controller.getTeamDiveEntriesMap());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return champsInfo;
    }


    private boolean showMeetExistsDialog(String message)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Meet Results Exist");
        alert.setContentText(message + ".  Do you want to continue?");

        Optional<ButtonType> result = alert.showAndWait();
        return (result.get() == ButtonType.OK);
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
            Stage dialogStage = createDialogStage(page, "Results Dialog");

            ProcessResultsDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setResultDir(resultsDir);

            dialogStage.showAndWait();
            if (controller.dialogCancelled()) {
                gcslAppController.setStatus("Process Meet Results cancelled.");
            }
            else {
                resultFiles = controller.getResultFiles();
            }
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
            Stage dialogStage = createDialogStage(page, "Roster Dialog");

            ProcessRosterDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setRosterDir(rosterDir);

            dialogStage.showAndWait();
            if (controller.dialogCancelled()) {
                gcslAppController.setStatus("Process Rosters cancelled.");
            }
            else {
                rosterFiles = controller.getRosterFiles();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return rosterFiles;
    }


    // Show the Rain Out Results dialog so that we can pair up rain out entries
    // into a single MeetResult object.  Return the MeetResults list
    // with the pairs of rainout entries collapsed.
    private List<MeetResults> showRainOutDialog(List<MeetResults> meetResults)
    {
        List<MeetResults> updatedMeetResults;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(GcslApp.class.getResource("view/RainOutResults.fxml"));
            AnchorPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Rain Out Results");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            RainOutResultsController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setMeetResultsList(meetResults);

            dialogStage.showAndWait();

            updatedMeetResults = controller.getMeetResultsList();
        }
        catch (IOException e) {
            updatedMeetResults = meetResults;
            e.printStackTrace();
        }

        return updatedMeetResults;
    }


    private void startTask(Task task)
    {
        Thread backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }
}
