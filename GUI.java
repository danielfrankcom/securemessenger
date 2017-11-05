import java.io.FileInputStream;
import java.io.IOException;

import com.sun.media.jfxmediaimpl.platform.Platform;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
 

public class GUI extends Application { 

    @FXML
    private TextArea messages;

    private SampleController cont;

    @Override
    public void start(Stage stage) throws Exception{

        FileInputStream fxmlStream = new FileInputStream("messenger.fxml");
    
        FXMLLoader loader = new FXMLLoader();
        AnchorPane root = (AnchorPane) loader.load(fxmlStream);
        cont = (SampleController) loader.getController();
    
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Secure Messenger");
        stage.show();
        cont.setText("text");

        /*Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                Thread.sleep(2000);
                return null ;
            }
        };

        task.setOnSucceeded(event -> {
            System.out.println("printed!!!!!");
        });

        new Thread(task).run();*/

        /*javafx.application.Platform.runLater(() -> {
            
            try{
                Thread.sleep(2000);
            }catch(Exception e){

            }
            SampleController c = new SampleController();
            c.setText("test");
        });*/

    }  

    public GUI(){
        System.out.println("running g");
        //launch();
    }
    
    public void setText(String text){
        messages.setText(text);
    }

    public static void main(String[] args){
        System.out.println("test");
        //Class s = GUI.class;
        Application.launch(args);
        //setText("temp");
    }
} 