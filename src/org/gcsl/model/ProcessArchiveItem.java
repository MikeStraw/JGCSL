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
    private final SimpleStringProperty  scenario;
    private final SimpleBooleanProperty selected;

    // List of scenarios for processing the archive item.
    public enum Scenario {
        BYE_WEEK_ENTRIES("Bye Week Entries"),
        BYE_WEEK_RESULTS("Bye Week Results"),
        MEET_ENTRIES("Meet Entries"),
        MEET_RESULTS("Meet Results"),
        RAIN_OUT_ENTRIES("Rain Out Entries"),
        RAIN_OUT_RESULTS("Rain Out Results"),
        TEAM_ROSTER("Team Roster");

        private final String text;
        Scenario(final String scenarioText) { text = scenarioText; }

        @Override
        public String toString()  { return text; }
    }


    public ProcessArchiveItem(String filePath, String fileContents, boolean isSelected, Scenario processScenario)
    {
        File archiveFile = new File(filePath);

        contents = new SimpleStringProperty(fileContents);
        dirPath  = archiveFile.getParent();
        name     = new SimpleStringProperty(archiveFile.getName());
        scenario = new SimpleStringProperty(processScenario.toString());
        selected = new SimpleBooleanProperty(isSelected);
    }

    public String getContents()                  { return contents.get(); }
    public void   setContents(String newContent) { contents.set(newContent); }

    public String getDirectory()                 { return dirPath; }

    public String getName()                      { return name.get(); }
    public void   setName(String newName)        { name.set(newName); }

    public String getScenario()                  { return scenario.get(); }
    public void   setScenario(String newScenario){ this.scenario.set(newScenario); }

    public BooleanProperty selectedProperty()    { return selected; }
    public boolean getSelected()                 { return selected.get(); }
    public void setSelected(boolean isSelected)  { this.selected.set(isSelected); }

}
