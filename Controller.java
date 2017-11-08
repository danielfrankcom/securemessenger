import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/*
* Controller for GUI, handles all changes that user will see
* Can be updated externally, and notifies Messenger of typed messages/commands
*/
public class Controller implements Initializable{

    @FXML TextArea messages; //these 2 are passed from the FXML (GUI) file
    @FXML TextField input;
    @FXML CheckBox confidentiality;
    @FXML CheckBox integrity;
    @FXML CheckBox authentication;

    private Messenger messenger; //this stores the Messenger that owns the GUI
    
    /*
    * Necessary method that runs when GUI is initialized
    * @param       URL url (passed from JavaFX)
    * @param       ResourceBundle rb (passed from JavaFX)
    * @return      void
    */
    @Override
    public void initialize(URL url, ResourceBundle rb){
        //don't really have anything to do, but necessary to override
    }

    /*
    * Runs when enter key is pressed in text field
    * @param       ActionEvent a (not used but required)
    * @return      void
    */
    @FXML
    public void onEnter(ActionEvent a) throws Exception{

        String msg = input.getText(); //get field value

        if(msg.equals("")){
            return;
        }

        input.clear(); //clear field once stored
        
        if(msg.charAt(0) == ':'){ //if the input is a command
            messenger.command(msg.substring(1)); //strip the ':'
        }else{
            messenger.typed(msg); //pass on the message
        }

    }

    /*
    * Runs when a check box is modified
    * @param       ActionEvent a (not used but required)
    * @return      void
    */
    @FXML
    public void onClick(ActionEvent a) throws Exception{

        System.out.println("checked");

    }

    /*
    * Add a line to the messaging window (for external use)
    * @param       String message
    * @return      void
    */
    public void addText(String text){
        messages.appendText(text); //append to GUI text area
    }
    
    /*
    * Set local Messenger for passing on messages/commands
    * @param       Messenger mes (our owner class)
    * @return      void
    */
    public void setMessenger(Messenger mes){
        messenger = mes;
    }

}