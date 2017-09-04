package org.gcsl;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application
{
    private GcslApp app;

    @Override
    public void start(Stage primaryStage) throws Exception{
        app = new GcslApp(primaryStage, "view/RootLayout.fxml");
        app.start();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
