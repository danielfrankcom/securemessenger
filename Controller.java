import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

public class Controller implements Initializable{

    @FXML TextArea messages;
    
    @Override
    public void initialize(URL url, ResourceBundle rb){
        //runs when the application starts    
    }

    public void setText(String text){ //for use outside of controller
        messages.setText(text);
    }

}