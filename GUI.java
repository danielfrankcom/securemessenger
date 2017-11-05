import javafx.application.Application; 
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene; 
import javafx.scene.paint.Color; 
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.fxml.*;

public class GUI extends Application {    
   @Override
   public void start(Stage stage) throws Exception {
      Parent root = FXMLLoader.load(getClass().getResource("messenger.fxml"));
   
       Scene scene = new Scene(root, 600, 400);
   
       stage.setTitle("FXML Welcome");
       stage.setScene(scene);
       stage.show();
   }
   public static void main(String args[]){          
      launch(args);     
   }         
} 