package org.gcsl.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

// This class holds information about an archive file and whether it has been selected for processing
public class ProcessArchiveItem
{
    private final SimpleStringProperty  contents;
    private final SimpleStringProperty  path;
    private final SimpleBooleanProperty selected;

    public ProcessArchiveItem(String filePath, String fileContents, boolean isSelected)
    {
        contents = new SimpleStringProperty(fileContents);
        path     = new SimpleStringProperty(filePath);
        selected = new SimpleBooleanProperty(isSelected);
    }

    public String getContents()                  { return contents.get(); }
    public void   setContents(String newContent) { contents.set(newContent); }

    public String getPath()                      { return path.get(); }
    public void   setPath(String newPath)        { path.set(newPath); }

    public BooleanProperty selectedProperty()    { return selected; }
    public boolean getSelected()                 { return selected.get(); }
    public void setSelected(boolean isSelected)  { this.selected.set(isSelected); }

}
