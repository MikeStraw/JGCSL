package org.gcsl.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;

// This class holds information about an archive file and whether it has been selected for processing
public class ProcessArchiveItem
{
    private final SimpleStringProperty  contents;
    private final String                dirPath;
    private final SimpleStringProperty  name;
    private final SimpleBooleanProperty selected;

    public ProcessArchiveItem(String filePath, String fileContents, boolean isSelected)
    {
        File archiveFile = new File(filePath);

        contents = new SimpleStringProperty(fileContents);
        dirPath  = archiveFile.getParent();
        name     = new SimpleStringProperty(archiveFile.getName());
        selected = new SimpleBooleanProperty(isSelected);
    }

    public String getContents()                  { return contents.get(); }
    public void   setContents(String newContent) { contents.set(newContent); }

    public String getDirectory()                 { return dirPath; }

    public String getName()                      { return name.get(); }
    public void   setName(String newName)        { name.set(newName); }

    public BooleanProperty selectedProperty()    { return selected; }
    public boolean getSelected()                 { return selected.get(); }
    public void setSelected(boolean isSelected)  { this.selected.set(isSelected); }

}
