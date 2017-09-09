package org.gcsl.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextField;

import javafx.stage.Stage;
import org.gcsl.model.ProcessArchiveItem;


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

    private Stage dialogStage;

    private final ObservableList<ProcessArchiveItem> archiveItemsList = FXCollections.observableArrayList();

    public void setDialogStage(Stage stage) { dialogStage = stage; }
    public void setRosterDir(String dir)    { archiveDir.setText(dir); }

    @FXML private void initialize()
    {
        System.out.println("Inside ProcessRosterDialogController::initialize");
        // add some fake data
        archiveItemsList.add(new ProcessArchiveItem("DARTS-Roster004.ZIP", "CFILE01.CL2, HFILE001.HY3", true));
        archiveItemsList.add(new ProcessArchiveItem("Archive Name #2", "CFILE01.CL2, HFILE001.HY3", true));
        archiveItemsList.add(new ProcessArchiveItem("Archive Name #3", "CFILE01.CL2, HFILE001.HY3", true));
        archiveItemsList.add(new ProcessArchiveItem("Archive Name #4", "CFILE01.CL2, HFILE001.HY3", true));
        archiveItemsList.add(new ProcessArchiveItem("Archive Name #5", "CFILE01.CL2, HFILE001.HY3", true));
        archiveItemsList.add(new ProcessArchiveItem("NWGCS-OH-Roster004.ZIP", "CFILE01.CL2, HFILE001.HY3", true));
        archiveItemsList.add(new ProcessArchiveItem("WAVE-GS-004.sd3", "Worthington_Roster.sd3", true));

        archiveTable.setItems(archiveItemsList);
    }

    // *************** GUI Handler Methods

    @FXML private void handleBrowseButtonClick()
    {
        System.out.println("Browse Button Click");


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



}
