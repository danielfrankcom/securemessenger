import java.io.FileInputStream;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
 
public class GUI extends Application{ 

    private static Controller controller;

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

    }

    public static Controller getInstance(){ //starts the GUI and returns the controller

        new Thread("gui"){
            public void run(){
                Application.launch(GUI.class);
            }
        }.start();
        try{
            Boolean init = false;
            while(!init){

                try{ //not very elegant but under a time crunch so good for now
                    controller.setText("");
                    init = true;
                }catch(NullPointerException e){
                    Thread.sleep(100);
                }

            }
        }catch(Exception e){
            //we should deal with this later
        }
        return controller;

    }

} 