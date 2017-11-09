import java.io.FileInputStream;
import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
 
/**
* Initializes and sets up the GUI, passes the controller to the parent Messenger class
*/
public class GUI extends Application{ 

    /**
    * stores controller (only accessible when start method runs)
    */
    private static Controller controller;
    /**
    * stores Messenger id
    */
    private static String id;

    /**
    * Necessary method that runs when GUI is initialized
    * @param stage passed by JavaFX
    */
    @Override
    public void start(Stage stage){

        FileInputStream fxmlStream = null;
        try{
            fxmlStream = new FileInputStream("messenger.fxml"); //load layout file
        }catch(Exception e){
            System.out.println("FXML file error");
        }
        FXMLLoader loader = new FXMLLoader();
        AnchorPane root = null;
        try{
            root = (AnchorPane) loader.load(fxmlStream);
        }catch(IOException e){
            System.out.println("FXML loader error");
        }

        controller = (Controller) loader.getController(); //access and set instance controller
    
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(id); //id from Messenger
        stage.show(); //show GUI

    }

    /**
    * Gracefully handle exit other than by command, called by JavaFX on application close
    */
    @Override
    public void stop(){

        System.exit(0); //stop all threads including connection listener and RMI
        
    }

    /**
    * For use by Messenger class, shares objects between messenger/controller
    * @param mes owner, to be passed along to controller
    * @param localID value for title bar
    * @return object passed to owner to allow direct access to controller
    */
    public static Controller getInstance(Messenger mes, String localID){ //starts the GUI and returns the controller

        id = localID; //set instance variable

        new Thread("gui"){ //create a thread as run() only ends when GUI quits
            public void run(){
                Application.launch(GUI.class); //start initialization process
            }
        }.start();

        try{
            Boolean init = false;
            while(!init){ //while the GUI is not loaded

                try{ //not very elegant but under a time crunch so good for now
                    controller.addText(""); //check if we get an error trying to add text
                    init = true;
                }catch(NullPointerException e){
                    Thread.sleep(100); //wait a bit to avoid busywait
                }

            }
            controller.setMessenger(mes); //pass Messenger class to controller
        }catch(Exception e){
            //we must catch this to avoid errors, but should deal with it later
        }

        return controller; //send to Messenger class

    }

} 