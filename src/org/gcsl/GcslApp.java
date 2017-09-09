package org.gcsl;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gcsl.view.ProcessRosterDialogController;
import org.gcsl.view.RootLayoutController;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
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
        System.out.println("GcslApp::processRosters");
        showProcessRosterDialog();
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
        String version = "";
        try(Statement statement = dbConn.createStatement()) {
            try(ResultSet rs = statement.executeQuery("select sqlite_version();")) {
                version = rs.getString(1);
                System.out.println(version);
            }
        }
        return version;
    }


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

    private boolean showProcessRosterDialog()
    {
        boolean rc = true;
        String rosterDir = config.getProperty("rosters_dir");

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
        }
        catch (IOException e) {
            e.printStackTrace();
            rc = false;
        }

        return rc;
    }
}
