package org.gcsl.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ChampsExceptionsDialogController
{
    @FXML private Button    browseDiveFileBtn;
    @FXML private Button    browseEntriesDirBtn;
    @FXML private Button    browseReportsDirBtn;
    @FXML private Button    cancelBtn;
    @FXML private TextField diveEntriesFile;
    @FXML private TextField entryDir;
    @FXML private Button    idDiversBtn;
    @FXML private TextField reportsDir;
    @FXML private Button    runReportBtn;


    private boolean dialogCancelled = true;
    private Stage   dialogStage;

    // ********************************************************************************
    // ***************     Public Methods
    // ********************************************************************************
    public boolean dialogCancelled()                  { return dialogCancelled; }
    public void    setDialogStage(Stage stage)        { dialogStage = stage; }
    public void    setEntriesDir(String dir)          { entryDir.setText(dir); }
    public void    setReportsDir(String dir)          { reportsDir.setText(dir);}


    // ********************************************************************************
    // ***************     Private GUI methods
    // ********************************************************************************
    @FXML private void handleCancelButtonClick()
    {
        dialogCancelled = true;
        dialogStage.close();
    }

    @FXML private void handleDiveFileBrowswButtonClick()
    {
        FileChooser fileChooser = new FileChooser();
        File initialDir = new File(entryDir.getText());

        if (initialDir.exists()  &&  initialDir.isDirectory()) {
            fileChooser.setInitialDirectory(initialDir);
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            diveEntriesFile.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML private void handleEntriesBrowseButtonClick()
    {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File initialDir = new File(entryDir.getText());

        if (initialDir.exists()  &&  initialDir.isDirectory()) {
            dirChooser.setInitialDirectory(initialDir);
        }

        File newDir = dirChooser.showDialog(dialogStage.getOwner());
        if (newDir != null  &&  ! newDir.equals(initialDir)) {
            entryDir.setText(newDir.getAbsolutePath());
        }
    }

    @FXML private void handleIdDiversButtonClick()
    {
        System.out.println("handleIdDiversButtonClick");
    }

    @FXML private void handleReportsBrowseButtonClick()
    {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File initialDir = new File(reportsDir.getText());

        if (initialDir.exists()  &&  initialDir.isDirectory()) {
            dirChooser.setInitialDirectory(initialDir);
        }

        File newDir = dirChooser.showDialog(dialogStage.getOwner());
        if (newDir != null  &&  ! newDir.equals(initialDir)) {
            reportsDir.setText(newDir.getAbsolutePath());
        }
    }

    @FXML private void handleRunReportsButtonClick()
    {
        System.out.println("handleRunReportButtonClick");
        dialogCancelled = false;
        dialogStage.close();
    }
}
