package org.gcsl.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.util.Callback;
import org.gcsl.model.ProcessArchiveItem;

import java.util.stream.Collectors;
import java.util.stream.Stream;

// Draw a combo box with the different ProcessArchive scenarios
public class ComboBoxCellFactory implements Callback
{
    private final ObservableList<String> scenarios = FXCollections.observableArrayList(Stream.of(ProcessArchiveItem.Scenario.values())
                                                                                             .map(ProcessArchiveItem.Scenario::toString)
                                                                                             .collect(Collectors.toList()));

    @Override
    public Object call(Object param)  // call(CellDataFeatures<Customer, String> param)
    {
        ComboBoxTableCell<ProcessArchiveItem,String> comboBoxCell = new ComboBoxTableCell<>(scenarios);
        return comboBoxCell;
    }
}