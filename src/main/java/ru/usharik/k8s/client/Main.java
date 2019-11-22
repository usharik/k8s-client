package ru.usharik.k8s.client;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
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

    public final static CoreV1Api coreApi = initClient();

    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("FXML TableView Example");

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

    private static CoreV1Api initClient() {
        logger.info("Initializing Kubernetes client");

        ApiClient client = Config.fromUrl("http://localhost:8888");
        Configuration.setDefaultApiClient(client);
        return new CoreV1Api(client);
    }
}
