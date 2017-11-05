import java.io.FileInputStream;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
 
public class GUI extends Application{ 

    private Controller controller;

    @Override
    public void start(Stage stage) throws Exception{

        FileInputStream fxmlStream = new FileInputStream("messenger.fxml");
        FXMLLoader loader = new FXMLLoader();
        AnchorPane root = (AnchorPane) loader.load(fxmlStream);
        controller = (Controller) loader.getController();
    
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Secure Messenger");
        stage.show();
        controller.setText("text");

    }  

    public GUI(){
        //launch(); //not sure if we can make this work just yet
    }

    public static void main(String[] args){
        Application.launch(args);
    }

} 