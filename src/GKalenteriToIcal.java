import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;
import java.lang.Thread;

public class GKalenteriToIcal extends Application {
    private static int FIELD_WIDTH = 500;
    private static int GRID_VGAP = 10;
    private static int GRID_HGAP = 10;

    private ListView<CheckBox> coursesListView;
    private ArrayList<CheckBox> linkCheckBoxes;
    private ArrayList<String[]> links;
    private Vcalendar vcalendar;
    private TextField selectedFileTextField;
    private ComboBox privacyComboBox;
    private CheckBox dontDuplicateCheckBox;
    FileChooser fileChooser = new FileChooser();
    private File outputFile;

    public static void showAlert(String title, String message, AlertType type) {
      Alert alert = new Alert(type);
      alert.setTitle(title);
      alert.setHeaderText(title);
      alert.setContentText(message);
      alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
      alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Kalenteri to ICAL");
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(GRID_VGAP);
        grid.setVgap(GRID_HGAP);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text scenetitle = new Text("Kalenteri to ICAL");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));

        Label addressLabel = new Label("Give clendar address:");

        TextField addressTextField = new TextField();
        addressTextField.setPrefWidth(FIELD_WIDTH);
        Button fetchButton = new Button("Fetch");
        fetchButton.setOnAction(new EventHandler<ActionEvent>() {
          @Override public void handle(ActionEvent e) {
            coursesListView.getItems().clear();
            Alert alert = new Alert(AlertType.NONE);
            alert.setTitle("Fetching courses...");
            alert.getDialogPane().setMinWidth(300);
            ProgressBar pb = new ProgressBar();
            alert.getDialogPane().setContent(pb);
            Task<Void> task = new Task<Void>() {
              @Override
              public Void call() throws InterruptedException {
                links = new ArrayList<String[]>();
                links = AsioParse.extractLinks(addressTextField.getText(), false);
                linkCheckBoxes = new ArrayList<CheckBox>();
                for(String[] link : links) {
                  CheckBox cb = new CheckBox(link[0]);
                  cb.setSelected(true);
                  linkCheckBoxes.add(cb);
                }
                return null;
              }
            };
            task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
              @Override
              public void handle(WorkerStateEvent t) {
                alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
                alert.close();
                coursesListView.getItems().addAll(linkCheckBoxes);
                showAlert(
                    "Courses fetched",
                    "All available courses fetched. Found "
                    + linkCheckBoxes.size()
                    + " occurences.",
                    AlertType.INFORMATION
                );
              }
            });
            alert.show();
            pb.setProgress(-1F);
            pb.progressProperty().bind(task.progressProperty());
            new Thread(task).start();
          }
        });

        coursesListView = new ListView<CheckBox>();

        privacyComboBox = new ComboBox();
        privacyComboBox.getItems().addAll(
            "PUBLIC",
            "CONFIDENTIAL",
            "PRIVATE"
        );
        privacyComboBox.getSelectionModel().selectFirst();

        dontDuplicateCheckBox = new CheckBox("Dont duplicate");

        selectedFileTextField = new TextField();
        selectedFileTextField.setPrefWidth(300);
        selectedFileTextField.setEditable(false);

        Button selectOutputFileButton = new Button("Select output file...");
        selectOutputFileButton.setOnAction(new EventHandler<ActionEvent>() {
          @Override public void handle(ActionEvent e) {
            fileChooser.setTitle("Open output file");
            fileChooser.setInitialFileName("file.ics");
            outputFile = fileChooser.showOpenDialog(primaryStage);
            selectedFileTextField.setText(outputFile.getAbsolutePath());
          }
        });

        Button fetchSelectedButton = new Button("Fetch selected courses and write to file");
        fetchSelectedButton.setOnAction(new EventHandler<ActionEvent>() {
          @Override public void handle(ActionEvent e) {
            if (outputFile == null) {
                showAlert("No output file",
                          "You have to select an output file",
                          AlertType.ERROR);
                return;
            }
            try {
              outputFile.createNewFile();
            }
            catch (IOException ex) {
              showAlert(
                  "Outputfile not writable",
                  ex.getMessage(),
                  AlertType.ERROR
              );
              return;
            }
            Alert alert = new Alert(AlertType.NONE);
            alert.setTitle("Fetching events...");
            alert.getDialogPane().setMinWidth(300);
            ProgressBar pb = new ProgressBar();
            alert.getDialogPane().setContent(pb);
            Task<Integer> task = new Task<Integer>() {
              @Override
              public Integer call() throws InterruptedException {
                int processLength = linkCheckBoxes.size();
                updateProgress(0, processLength);
                vcalendar = new Vcalendar("2.0", "sikkela");
                int total = 0;
                for (int i = 0; i < linkCheckBoxes.size(); i++) {
                  if (linkCheckBoxes.get(i).isSelected()) {
                    Vevent[] vevents = AsioParse.fetchCourseTimetable(
                        links.get(i)[1],
                        dontDuplicateCheckBox.isSelected(),
                        privacyComboBox.getValue().toString(),
                        false
                    );
                    total += vevents.length;
                    for (Vevent vevent : vevents) {
                        vcalendar.addEvent(vevent);
                    }
                  }
                  updateProgress(i, processLength);
                }

                // Write result to file.
                try {
                    PrintWriter out = new PrintWriter(outputFile);
                    out.write(vcalendar.toString());
                    out.close();
                }
                catch (Exception ex) {
                  showAlert(
                      "Outputfile not writable",
                      ex.getMessage(),
                      AlertType.ERROR
                  );
                  return 0;
                }
                return total;
              }
            };
            task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
              @Override
              public void handle(WorkerStateEvent t) {
                alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
                alert.close();
                showAlert(
                    "Events written",
                    "Selected events have been written to file. A total of "
                    + task.getValue() + " events written.",
                    AlertType.INFORMATION
                );
              }
            });
            alert.show();
            pb.setProgress(-1F);
            pb.progressProperty().bind(task.progressProperty());
            new Thread(task).start();
          }
        });

        grid.add(scenetitle, 0, 0, 2, 1);
        grid.add(addressLabel, 0, 1, 2, 1);
        grid.add(addressTextField, 0, 2, 2, 1);
        grid.add(fetchButton, 0, 3, 2, 1);
        grid.add(coursesListView, 0, 4, 2, 1);
        grid.add(privacyComboBox, 0, 5, 2, 1);
        grid.add(dontDuplicateCheckBox, 0, 6, 2, 1);
        grid.add(selectedFileTextField, 0, 7);
        grid.add(selectOutputFileButton, 1, 7);
        grid.add(fetchSelectedButton, 0, 8, 2, 1);

        Scene scene = new Scene(grid);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
