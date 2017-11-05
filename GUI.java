import java.io.FileInputStream;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
 
/*
* Initializes and sets up the GUI
* Passes the controller to the owner (Messenger)
*/
public class GUI extends Application{ 

    private static Controller controller; //stores controller (only accessible when start method runs)
    private static String id; //stores Messenger id

    /*
    * Necessary method that runs when GUI is initialized
    * @param       Stage stage (passed by JavaFX)
    * @return      void
    */
    @Override
    public void start(Stage stage) throws Exception{

        FileInputStream fxmlStream = new FileInputStream("messenger.fxml"); //load layout file
        FXMLLoader loader = new FXMLLoader();
        AnchorPane root = (AnchorPane) loader.load(fxmlStream);

        controller = (Controller) loader.getController(); //access and set instance controller
    
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(id); //id from Messenger
        stage.show(); //show GUI

    }

    /*
    * Gracefully handle exit other than by command
    * @return      void
    */
    @Override //called by JavaFX when application closed
    public void stop(){
        System.exit(0); //stop all threads including connection listener and RMI
    }

    /*
    * For use by Messenger class, shares objects between messenger/controller
    * @param       Messenger mes (owner, to be passed along to controller)
    * @param       String id (for title bar)
    * @return      Controller (to be passed to owner to allow direct access to controller)
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