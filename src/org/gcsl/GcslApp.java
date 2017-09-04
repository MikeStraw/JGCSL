package org.gcsl;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

class GcslApp
{
    private Stage stage;

    GcslApp(Stage primaryStage, String fxmlPath) throws IOException
    {
        stage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage.setTitle("GCSL Registrar");
        stage.setScene(new Scene(root, 600, 400));
    }

    void start()
    {
        stage.show();
    }

}
