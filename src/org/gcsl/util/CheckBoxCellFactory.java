package org.gcsl.util;

import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.TableCell;
import javafx.util.Callback;
import org.gcsl.model.ProcessArchiveItem;


public class CheckBoxCellFactory implements Callback
{
    @Override
    public TableCell call(Object param) {
        CheckBoxTableCell<ProcessArchiveItem,Boolean> checkBoxCell = new CheckBoxTableCell();
        return checkBoxCell;
    }
}