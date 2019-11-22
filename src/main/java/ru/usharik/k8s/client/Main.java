package ru.usharik.k8s.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.usharik.k8s.client.controller.PodsGridController;

public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting application");

        primaryStage.setTitle("K8S Pods monitor");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/table_view.fxml"));
        Parent root = loader.load();
        PodsGridController controller = loader.getController();
        controller.setStage(primaryStage);

        Scene myScene = new Scene(root);
        primaryStage.setScene(myScene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
