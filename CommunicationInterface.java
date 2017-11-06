import java.rmi.Remote;

/*
* Defines what methods are accessible over RMI
*/
public interface CommunicationInterface extends Remote { 
    String getID() throws Exception; //get the id string of the object
    void createPub(byte[] otherPub) throws Exception; //create a public key to otherPub spec
    void sharePub(byte[] otherPub) throws Exception; //share the created public key
    void message(String msg) throws Exception; //send a message to the object
    void init(String other) throws Exception; //initialize communication with the object
} 