package org.gcsl.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.gcsl.model.ProcessArchiveItem;
import org.gcsl.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;


public class ProcessChampEntriesDialogController
{
    @FXML private Label  archiveDir;
    @FXML private Button cancelBtn;
    @FXML private Button processBtn;

    // Archive Table and columns
    @FXML private TableView<ProcessArchiveItem> archiveTable;
    @FXML private TableColumn<ProcessArchiveItem, String>  nameColumn;
    @FXML private TableColumn<ProcessArchiveItem, String>  contentsColumn;
    @FXML private TableColumn<ProcessArchiveItem, Boolean> selectedColumn;

    private boolean dialogCancelled = true;
    private Stage dialogStage;
    private final ObservableList<ProcessArchiveItem> archiveItemsList = FXCollections.observableArrayList();
    private List<ProcessArchiveItem> entryFiles = Collections.emptyList();

    // ********************************************************************************
    // ***************     Public Methods
    // ********************************************************************************
    public boolean dialogCancelled()                 { return dialogCancelled; }
    public List<ProcessArchiveItem> getEntryFiles()  { return entryFiles; }
    public void setDialogStage(Stage stage)          { dialogStage = stage; }
    public void setEntryDir(String dir)              { archiveDir.setText(dir); }


    // ********************************************************************************
    // ***************     Private GUI methods
    // ********************************************************************************

    @FXML private void handleCancelButtonClick()
    {
        dialogCancelled = true;
        dialogStage.close();
    }

    @FXML private void handleProcessButtonClick()
    {
        dialogCancelled = false;
        entryFiles = archiveTable.getItems().stream().filter(t -> t.getSelected()).collect(toList());
        dialogStage.close();
    }

    @FXML private void initialize()
    {
        System.out.println("PCED:  archiveItemsList size = " + archiveItemsList.size());
        archiveTable.setItems(archiveItemsList);

        // Set up archive directory label listener.  listener called with each change to text field
        //   + happens when setText called <-- GOOD
        //   + happens with each character typed into field <-- BAD
        archiveDir.textProperty().addListener( (observable, oldValue, newValue) -> {
            // For convenience, call populateTable if oldValue is empty (ie during initialization)
            System.out.printf("archiveDir listner, oldVal=%s, newVal=%s %n", oldValue, newValue);
            if (oldValue.isEmpty() &&  ! newValue.isEmpty()) {
                populateArchiveTable(new File(newValue));
            }
        });
    }


    // ********************************************************************************
    // ***************     Private Methods
    // ********************************************************************************
    private void populateArchiveTable(File archiveDir)
    {
        // invalidate existing list
        archiveItemsList.clear();

        // Look for all ZIP and SD3 files
        String       extensions    = ".sd3 .zip";
        List<String> extensionList = new ArrayList<>(Arrays.asList(extensions.split(" ")));
        List<File>   rosterFiles   = Utils.getFilesFromDirectory(archiveDir, extensionList);
        System.out.printf("populateArchiveTable:  dir=%s, num archive files=%d %n",
                          archiveDir.getPath(), rosterFiles.size());

        for (File file : rosterFiles) {
            List<String> filesInArchive = Utils.getFileNamesFromArchive(file);
            String contents = String.join(", ", filesInArchive);
            archiveItemsList.add(new ProcessArchiveItem(file.getPath(), contents, true,
                                                        ProcessArchiveItem.Scenario.TEAM_ROSTER));
        }
    }
}

