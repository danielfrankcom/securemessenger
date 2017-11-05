import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class Controller implements Initializable{

    @FXML TextArea messages;
    @FXML TextField input;
    private Messenger messenger;
    
    @Override
    public void initialize(URL url, ResourceBundle rb){
        //runs when the application starts    
    }

    public void addText(String text){ //for use outside of controller
        messages.appendText(text);
    }

    public void setMessenger(Messenger mes){
        messenger = mes;
    }

    @FXML
    public void onEnter(ActionEvent a) throws Exception{
        String msg = input.getText();
        input.clear();
        if(msg.charAt(0) == ':'){
            messenger.command(msg.substring(1));
        }else{
            messenger.typed(msg);
        }
    }

}