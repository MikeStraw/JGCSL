package org.gcsl.view;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextField;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.gcsl.model.ProcessArchiveItem;
import org.gcsl.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ProcessRosterDialogController
{
    @FXML private TextField archiveDir;
    @FXML private Button    browseBtn;
    @FXML private Button    cancelBtn;
    @FXML private Button    processBtn;

    // Archive Table and columns
    @FXML private TableView<ProcessArchiveItem> archiveTable;
    @FXML private TableColumn<ProcessArchiveItem, String> pathColumn;
    @FXML private TableColumn<ProcessArchiveItem, String> contentsColumn;
    @FXML private TableColumn<ProcessArchiveItem, Boolean> selectedColumn;

    private boolean archiveDirIsDirty = false;
    private Stage dialogStage;
    private final ObservableList<ProcessArchiveItem> archiveItemsList = FXCollections.observableArrayList();

    // ***************     Public Methods
    public void setDialogStage(Stage stage) { dialogStage = stage; }
    public void setRosterDir(String dir)    { archiveDir.setText(dir); }

    // ***************     Private GUI methods
    @FXML private void initialize()
    {
        System.out.println("Inside ProcessRosterDialogController::initialize");

        archiveTable.setItems(archiveItemsList);

        // set up archive directory textfield listeners
        // listener called with each change to text field
        //   + happens when setText called
        //   + happens with each character typed into field
        archiveDir.textProperty().addListener( (observable, oldValue, newValue) -> {

            archiveDirIsDirty = true;
            // For convenience, call populateTable if oldValue is empty (ie during initialization)
            if (oldValue.isEmpty() &&  ! newValue.isEmpty()) {
                populateArchiveTable(new File(newValue));
            }
        });

        // OnAction happens when Enter hit while focus is in textfield
//        archiveDir.setOnAction( (event) -> {
//            System.out.println("archiveDir OnAction");
//        });

        //
        archiveDir.focusedProperty().addListener( (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            focusState(newValue);
        });
    }

    private void focusState(boolean hasFocus)
    {
        if (! hasFocus  &&  archiveDirIsDirty) {
            populateArchiveTable(new File(archiveDir.getText()));
        }
    }


    // Called when users hits Enter from the textfield
    @FXML private void handleArchiveDirAction(ActionEvent event)
    {
        browseBtn.requestFocus();
        if (archiveDirIsDirty) {
            populateArchiveTable(new File(archiveDir.getText()));
        }
    }


    // *************** GUI Handler Methods

    @FXML private void handleBrowseButtonClick()
    {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File  initialDir = new File(archiveDir.getText());

        if (initialDir.exists()  &&  initialDir.isDirectory()) {
            dirChooser.setInitialDirectory(initialDir);
        }

        File newDir = dirChooser.showDialog(dialogStage.getOwner());
        if (newDir != null  &&  ! newDir.equals(initialDir)) {
            archiveDir.setText(newDir.getAbsolutePath());
            populateArchiveTable(newDir);
        }
    }
    @FXML private void handleCancelButtonClick()
    {
        dialogStage.close();
    }

    @FXML private void handleProcessButtonClick()
    {
        int size = archiveTable.getItems().size();
        System.out.printf("Process Button Click: size=%d %n", size);

        archiveTable.getSelectionModel().select(0);
        ProcessArchiveItem item = archiveTable.getSelectionModel().getSelectedItem();
        System.out.printf("Archive 0: path=%s, contents=%s, selected=%s %n", item.getPath(), item.getContents(), item.getSelected());
    }

    // ***************     Private Methods
    private void populateArchiveTable(File archiveDir)
    {
        // invalidate existing list
        archiveItemsList.clear();

        // Look for all ZIP and SD3 files
        String       extensions    = ".sd3 .zip";
        List<String> extensionList = new ArrayList<>(Arrays.asList(extensions.split(" ")));
        List<File>   rosterFiles   = Utils.getFilesFromDirectory(archiveDir, extensionList);
        System.out.printf("populateArchiveTable:  path=%s, num archive files=%d %n",
                          archiveDir.getPath(), rosterFiles.size());

        for (File file : rosterFiles) {
            List<String> filesInArchive = Utils.getFileNamesFromArchive(file);
            String contents = String.join(", ", filesInArchive);
            archiveItemsList.add(new ProcessArchiveItem(file.getName(), contents, true));
        }

        archiveDirIsDirty = false;
        browseBtn.requestFocus();  // remove focus from text field when dialog first displayed
    }

}
