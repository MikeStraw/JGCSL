package org.gcsl.view;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.gcsl.db.AthleteDbo;
import org.gcsl.model.Athlete;
import org.gcsl.model.TeamDiveEntries;
import org.gcsl.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChampsExceptionsDialogController
{
    @FXML
    private Button browseDiveFileBtn;
    @FXML
    private Button browseEntriesDirBtn;
    @FXML
    private Button browseReportsDirBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private TextField diveEntriesFile;
    @FXML
    private TextField entryDir;
    @FXML
    private Button idDiversBtn;
    @FXML
    private TextField reportsDir;
    @FXML
    private Button runReportBtn;

    private Connection db;
    private boolean dialogCancelled = true;
    private Stage dialogStage;
    private Map<Integer, TeamDiveEntries> teamDiveEntriesMap = new HashMap<>();

    // ********************************************************************************
    // ***************     Public Methods
    // ********************************************************************************
    public boolean dialogCancelled()                { return dialogCancelled; }
    public String  getEntriesDir()                  { return entryDir.getText(); }
    public String  getReportsDir()                  { return reportsDir.getText(); }
    public Map<Integer, TeamDiveEntries>
                   getTeamDiveEntriesMap()          { return teamDiveEntriesMap; }

    public void setDbConnection(Connection conn)    { db = conn; }
    public void setDialogStage(Stage stage)         { dialogStage = stage;  }
    public void setEntriesDir(String dir)           { entryDir.setText(dir); }
    public void setReportsDir(String dir)           { reportsDir.setText(dir); }


    // ********************************************************************************
    // ***************     Private GUI methods
    // ********************************************************************************
    @FXML
    private void handleCancelButtonClick()
    {
        dialogCancelled = true;
        dialogStage.close();
    }

    @FXML
    private void handleDiveFileBrowseButtonClick()
    {
        FileChooser fileChooser = new FileChooser();
        File initialDir = new File(entryDir.getText());

        if (initialDir.exists() && initialDir.isDirectory()) {
            fileChooser.setInitialDirectory(initialDir);
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            diveEntriesFile.setText(selectedFile.getAbsolutePath());
            idDiversBtn.setDisable(false);
        }
    }

    @FXML
    private void handleEntriesBrowseButtonClick()
    {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File initialDir = new File(entryDir.getText());

        if (initialDir.exists() && initialDir.isDirectory()) {
            dirChooser.setInitialDirectory(initialDir);
        }

        File newDir = dirChooser.showDialog(dialogStage.getOwner());
        if (newDir != null && !newDir.equals(initialDir)) {
            entryDir.setText(newDir.getAbsolutePath());
        }
    }

    @FXML
    private void handleIdDiversButtonClick()
    {
        if (teamDiveEntriesMap.size() > 0) {
            teamDiveEntriesMap.clear();
        }
        try {
            File entryFile = new File(diveEntriesFile.getText());
            identifyDivers(entryFile);
            showOrphans();
            runReportBtn.setDisable(false);
        }
        catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Caught Exceptions");
            alert.setContentText("Exception occurred ... " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleReportsBrowseButtonClick()
    {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File initialDir = new File(reportsDir.getText());

        if (initialDir.exists() && initialDir.isDirectory()) {
            dirChooser.setInitialDirectory(initialDir);
        }

        File newDir = dirChooser.showDialog(dialogStage.getOwner());
        if (newDir != null && !newDir.equals(initialDir)) {
            reportsDir.setText(newDir.getAbsolutePath());
        }
    }

    @FXML
    private void handleRunReportsButtonClick()
    {
        if (entryDir.getText().isEmpty()  ||  reportsDir.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Missing Input");
            alert.setContentText("Champs Entry and Reports directory must not be empty");
            alert.showAndWait();
        }
        else {
            dialogCancelled = false;
            dialogStage.close();
        }
    }

    @FXML
    private void initialize()
    {
        idDiversBtn.setDisable(true);
        runReportBtn.setDisable(true);
    }

    // ********************************************************************************
    // ***************     Private Helper methods
    // ********************************************************************************
    private TeamDiveEntries getTeamDiveEntries(int teamId)
    {
        if (! teamDiveEntriesMap.containsKey(teamId))  {
            teamDiveEntriesMap.put(teamId, new TeamDiveEntries(teamId));
        }
        return teamDiveEntriesMap.get(teamId);
    }


    private void identifyDivers(File diveEntryFile) throws IOException, SQLException
    {
        System.out.println("identifyDivers()");

        try (BufferedReader bufReader = new BufferedReader(new FileReader(diveEntryFile))){
            String entryLine;
            System.out.println("Reading entry file ...");
            while ((entryLine = bufReader.readLine()) != null) {
                Athlete diveAthlete = parseEntry(entryLine);
                int teamId = diveAthlete.getTeamId();
                TeamDiveEntries teamEntries = getTeamDiveEntries(teamId);

                Athlete diverFromDb = AthleteDbo.findByNameOrId(db, diveAthlete);
                if (diverFromDb == null) {
                    teamEntries.addOrphan(entryLine);
                }
                else {
                    teamEntries.addDiver(diverFromDb);
                }
            }
        }
    }

    private String getOrphanList()
    {
        StringBuilder sb = new StringBuilder();

        teamDiveEntriesMap.forEach( (teamId, diveEntries) -> {
            List<String> orphans = diveEntries.getOrphans();
            orphans.forEach(orphan -> sb.append(orphan).append("\n"));
        });

        return sb.toString();
    }

    private Athlete parseEntry(String entryInfo) throws IOException
    {
        // Each line is of the form:  team-id, last, first, gender, age-group, athlete-id (optional)
        // Convert each dive entry into an Athlete, then check the database.
        String[] tokens = entryInfo.split(",");
        if (tokens.length < 5) {
            throw new IOException("Invalid entry, not enough tokens:  " + entryInfo);
        }
        int    teamId    = Integer.parseInt(tokens[0]);
        String lastName  = tokens[1];
        String firstName = tokens[2];
        String gender    = tokens[3];
        //String ageGrp  = tokens[4];
        int    athleteId = Utils.INVALID_ID;
        if (tokens.length > 5) {
            athleteId = Integer.parseInt(tokens[5]);
        }
        String fullName   = lastName + ", " + firstName;
        String dob        = "";
        String lastUpdate = "";

        return new Athlete(athleteId, fullName, gender, dob, teamId, lastUpdate);
    }

    private void showOrphans()
    {
        String orphanInfo = getOrphanList();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Dive Orphans");
        alert.setHeaderText("Dive Orphan List");
        if (orphanInfo.isEmpty()) {
            alert.setContentText("No orphans found.");
        }
        else {
            alert.setContentText(orphanInfo);
        }
        alert.show();
    }
}
