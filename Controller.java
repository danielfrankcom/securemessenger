import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
* Controller for GUI, handles all changes that user will see, can be updated externally, notifies Messenger of typed messages/commands
*/
public class Controller implements Initializable{

    /**
    * Message display area
    */
    @FXML TextArea messages;
    /**
    * User input field
    */
    @FXML TextField input;
    /**
    * Confidentiality checkbox
    */
    @FXML CheckBox confidentiality;
    /**
    * Confidentiality boolean
    */
    Boolean confidentialityChecked = false;
    /**
    * Integrity checkbox
    */
    @FXML CheckBox integrity;
    /**
    * Integrity boolean
    */
    Boolean integrityChecked = false;
    /**
    * Authentication checkbox
    */
    @FXML CheckBox authentication;
    /**
    * Authentication checkbox
    */
    Boolean authenticationChecked = false;
    /**
    * Messenger object to store the parent
    */
    private Messenger messenger;
    
    /**
    * Necessary method that runs when GUI is initialized
    * @param url passed from JavaFX
    * @param rb passed from JavaFX
    */
    @Override
    public void initialize(URL url, ResourceBundle rb){
        //don't really have anything to do, but necessary to override
    }

    /**
    * Runs when enter key is pressed in text field
    * @param a passed from JavaFX
    */
    @FXML
    public void onEnter(ActionEvent a){

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

    /**
    * Runs when confidentiality check box is modified
    * @param a passed from JavaFX
    */
    @FXML
    public void onClickConfidentiality(ActionEvent a){

        confidentialityChecked = !confidentialityChecked;
        messenger.setFlags(new Boolean[]{confidentialityChecked, integrityChecked, authenticationChecked});

    }

    /**
    * Runs when integrity check box is modified
    * @param a passed from JavaFX
    */
    @FXML
    public void onClickIntegrity(ActionEvent a){

        integrityChecked = !integrityChecked;
        messenger.setFlags(new Boolean[]{confidentialityChecked, integrityChecked, authenticationChecked});

    }

    /**
    * Runs when authentication check box is modified
    * @param a passed from JavaFX
    */
    @FXML
    public void onClickAuthentication(ActionEvent a){

        authenticationChecked = !authenticationChecked;
        messenger.setFlags(new Boolean[]{confidentialityChecked, integrityChecked, authenticationChecked});

    }

    /**
    * Enable/disable checkboxes once connection is established/disconnected
    * @param value set checkbox disabled value to this
    */
    public void setCheckBoxes(Boolean value){

        confidentiality.setDisable(value);
        integrity.setDisable(value);
        authentication.setDisable(value);

    }

    /**
    * Add a line to the messaging window
    * @param text line to add to the messaging window
    */
    public void addText(String text){

        messages.appendText(text); //append to GUI text area

    }
    
    /**
    * Set local Messenger for passing on messages/commands
    * @param mes parent's class
    */
    public void setMessenger(Messenger mes){

        messenger = mes;

    }

}