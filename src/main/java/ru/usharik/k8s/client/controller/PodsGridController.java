package ru.usharik.k8s.client.controller;

import com.google.common.io.ByteStreams;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.usharik.k8s.client.logger.TextAreaAppender;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PodsGridController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PodsGridController.class);

    public static final String K8S_SERVICE_URL="K8S Service URL";
    public static final String K8S_CONFIG_FILE_PATH ="K8S config file path";

    @FXML
    private TextField serviceUrl;

    @FXML
    private TextArea logsArea;

    @FXML
    private ComboBox<String> cbType;

    @FXML
    private TextField podNameFilter;

    @FXML
    private TextField tenantNameFilter;

    @FXML
    private TableView<PodInfo> tableView;

    @FXML
    private TableColumn<PodInfo, String> name;

    @FXML
    private TableColumn<PodInfo, String> tenantName;

    @FXML
    private TableColumn<PodInfo, String> status;

    @FXML
    public TableColumn<PodInfo, Integer> restartCount;

    @FXML
    private TableColumn<PodInfo, DateTime> startTime;

    @FXML
    private TableColumn<PodInfo, Integer> minutesFromStart;

    private FilteredList<PodInfo> filteredPodList;

    private ObservableList<PodInfo> observablePodList = FXCollections.observableArrayList();

    private Stage stage;

    public void initialize(URL location, ResourceBundle resources) {
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        restartCount.setCellValueFactory(new PropertyValueFactory<>("restartCount"));
        startTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        tenantName.setCellValueFactory(new PropertyValueFactory<>("tenantName"));
        minutesFromStart.setCellValueFactory(new PropertyValueFactory<>("minutesFromStart"));

        cbType.getSelectionModel().selectFirst();

        TextAreaAppender.setTextArea(logsArea);

        try {
            observablePodList = FXCollections.observableArrayList(queryPodsList());
        } catch (Exception ex) {
            // to prevent application close at startup
        }
        filteredPodList = new FilteredList<>(observablePodList, p -> true);
        SortedList<PodInfo> sortedList = new SortedList<>(this.filteredPodList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private List<PodInfo> queryPodsList() {
        try {
            logger.info("Getting pods list from service {}", serviceUrl.getText());

            ApiClient client = Config.defaultClient();
            switch (cbType.getSelectionModel().getSelectedItem()) {
                case K8S_SERVICE_URL:
                    logger.info("Starting client based on K8S service URL");
                    client = Config.fromUrl(serviceUrl.getText());
                    break;
                case K8S_CONFIG_FILE_PATH:
                    logger.info("Starting client based on K8S config file");
                    client = Config.fromConfig(serviceUrl.getText());
                    break;
            }
            Configuration.setDefaultApiClient(client);
            return new CoreV1Api(client)
                    .listNamespacedPod("default", "false", null, null, null, null, null, null, null)
                    .getItems().stream()
                    .map(PodInfo::new)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            logger.error("", ex);

            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setHeaderText("Can't update pods list");
            errorAlert.setContentText(ex.getLocalizedMessage());
            errorAlert.showAndWait();

            throw new IllegalStateException(ex);
        }
    }

    public void applyFilter(ActionEvent actionEvent) {
        String podNamePattern = podNameFilter.getText();
        String tenantPattern = tenantNameFilter.getText();

        filteredPodList.setPredicate(
                pod -> (emptyOrNull(podNamePattern) || pod.getName().matches(podNamePattern))
                        && (emptyOrNull(tenantPattern) || pod.getTenantName().matches(tenantPattern)));
    }

    public void copyPodName(ActionEvent actionEvent) {
        PodInfo podInfo = tableView.getSelectionModel().getSelectedItem();
        ClipboardContent content = new ClipboardContent();
        content.putString(podInfo.getName());
        Clipboard.getSystemClipboard().setContent(content);
    }

    public void copyTenantName(ActionEvent actionEvent) {
        PodInfo podInfo = tableView.getSelectionModel().getSelectedItem();
        ClipboardContent content = new ClipboardContent();
        content.putString(podInfo.getTenantName());
        Clipboard.getSystemClipboard().setContent(content);
    }

    public void downloadLogs(ActionEvent actionEvent) {
        PodInfo podInfo = tableView.getSelectionModel().getSelectedItem();
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("LOG files (*.log)", "*.log");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName(podInfo.getName() + ".log");

        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (OutputStream os = new FileOutputStream(file)) {
                PodLogs logs = new PodLogs();
                InputStream is = logs.streamNamespacedPodLog(podInfo.getV1Pod());

                ByteStreams.copy(is, os);
                logger.info("Log is saved to file: {}", file);
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.open(file);
                }
                else {
                    logger.error("Desktop is not supported");
                }
            } catch (Exception e) {
                logger.error("", e);

                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setHeaderText("Logs download error");
                errorAlert.setContentText(e.getLocalizedMessage());
                errorAlert.showAndWait();
            }
        }
    }

    public void refresh(ActionEvent actionEvent) {
        observablePodList.setAll(queryPodsList());
    }

    public void showPodInfo(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() != 2) {
            return;
        }

        PodInfo podInfo = tableView.getSelectionModel().getSelectedItem();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setResizable(true);
        dialog.setHeaderText(podInfo.getName());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        TextArea textArea = new TextArea(podInfo.getV1Pod().toString());
        textArea.setEditable(false);
        Tab tab1 = new Tab("All info", textArea);

        textArea = new TextArea(podInfo.getV1Pod().getStatus().toString());
        textArea.setEditable(false);
        Tab tab2 = new Tab("Status"  , textArea);

        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);

        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
        dialog.show();
    }

    private static boolean emptyOrNull(String str) {
        return str == null || str.isEmpty();
    }

    public void choiceConfigFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            serviceUrl.setText(file.toString());
        }
    }
}
