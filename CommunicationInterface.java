import java.rmi.Remote;

/*
* Defines what methods are accessible over RMI
*/
public interface CommunicationInterface extends Remote { 
    String getID() throws Exception; //get the id string of the object
    void message(String msg) throws Exception; //send a message to the object
    void init(String other) throws Exception; //initialize communication with the object
} 