package org.gcsl.view;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.gcsl.model.MeetResults;
import org.gcsl.model.ProcessArchiveItem;
import org.gcsl.model.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RainOutResultsController
{
    @FXML private Button closeBtn;
    @FXML private Button pairBtn;
    @FXML private Label  pairings;
    @FXML private Button resetBtn;
    @FXML private ListView<String> rainOutEntryFiles;  // backing store for the multi-select list view

    private Stage dialogStage;
    private List<MeetResults> meetResults = new ArrayList<>();
    private List<Integer> rainListBoxToInputList = new ArrayList<>();
    private List< Pair<Integer, Integer> > rainoutPairs = new ArrayList<>();
    private List<Integer> removeIdxs = new ArrayList<>();

    public void setDialogStage(Stage stage)                  { dialogStage = stage; }
    public List<MeetResults> getMeetResultsList()            { return meetResults; }
    public void setMeetResultsList(List<MeetResults> meets)  { meetResults = meets; popListView(); }



    @FXML private void initialize()
    {
        System.out.println("RainOUutResultsController::initialize MR-size=" + meetResults.size());

        rainOutEntryFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setPairLableText();
    }

    @FXML private void handleCloseButtonClick()
    {
        // TODO refactor
        for (Pair<Integer, Integer> rainoutPair : rainoutPairs) {
            Integer listboxIdx1 = rainoutPair.getKey();
            Integer listboxIdx2 = rainoutPair.getValue();
            int     meetIdx1 = rainListBoxToInputList.get(listboxIdx1);
            int     meetIdx2 = rainListBoxToInputList.get(listboxIdx2);
            MeetResults meet1 = meetResults.get(meetIdx1);
            MeetResults meet2 = meetResults.get(meetIdx2);

            List<Team> meet2Teams = meet2.getTeams();
            meet1.addTeam(meet2Teams.get(0));
            removeIdxs.add(meetIdx2);
        }

        // remove the Meet Results that were merged "into" another meet, starting at the end
        Collections.sort(removeIdxs);
        Collections.reverse(removeIdxs);
        for (Integer idx : removeIdxs) {
            removeIdxs.remove(idx);
        }
        dialogStage.close();
    }

    @FXML private void handlePairButtonClick()
    {
        ObservableList<Integer> selectedIndices = rainOutEntryFiles.getSelectionModel().getSelectedIndices();

        if (selectedIndices.size() != 2) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Rain Out Pairing");
            alert.setContentText("Fewer or more than 2 meets were selected, can only pair two meets.");

            alert.showAndWait();
        }
        else {
            Pair<Integer, Integer> rainoutPair = new Pair<>(selectedIndices.get(0), selectedIndices.get(1));
            rainoutPairs.add(rainoutPair);
            setPairLableText();
        }
    }

    @FXML private void handleResetButtonClick()
    {
        rainoutPairs.clear();
        setPairLableText();
    }

    private void popListView()
    {
        for (int i=0; i<meetResults.size(); i++) {
            MeetResults meet = meetResults.get(i);
            if (meet.getResultsScenario() == ProcessArchiveItem.Scenario.RAIN_OUT_ENTRIES) {
                // We have a rain out entry meet, set the display name and record the
                // meetResults index.
                String meetIdentifier = meet.getName() + " - " + meet.getDate().toString();
                rainOutEntryFiles.getItems().add(meetIdentifier);
                rainListBoxToInputList.add(i);
            }
        }
    }

    private void setPairLableText()
    {
        if (rainoutPairs.isEmpty())  {  pairings.setText("No pairings"); }
        else {
            StringBuilder pairingText = new StringBuilder("Pairings: ");

            for (Pair<Integer, Integer> pair : rainoutPairs) {
                pairingText.append("<")
                           .append(pair.toString())
                           .append(">  ");
            }
            pairings.setText(pairingText.toString());
        }
    }
}
