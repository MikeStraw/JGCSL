package org.gcsl;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.gcsl.View.RootLayoutController;

import java.io.IOException;

public class GcslApp
{
    private RootLayoutController gcslAppController;
    private Stage stage;

    GcslApp(Stage primaryStage, String fxmlPath) throws IOException
    {
        FXMLLoader loader = new FXMLLoader();


        loader.setLocation(getClass().getResource(fxmlPath));
        BorderPane root = loader.load();
        gcslAppController = loader.getController();
        gcslAppController.setApp(this);  // allow communication from controller back to app

        stage = primaryStage;
        stage.setTitle("GCSL Registrar");
        stage.setScene(new Scene(root, 525, 405));
    }

    void start()
    {
        stage.show();
    }

}
