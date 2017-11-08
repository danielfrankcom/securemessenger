import java.rmi.Remote;

/*
* Defines what methods are accessible over RMI
*/
public interface CommunicationInterface extends Remote { 
    String getID(); //get the id string of the object
    void createPub(byte[] otherPub, CommunicationInterface other); //create a public key to otherPub spec
    void share(byte[] otherPub, byte[] otherParams); //share the created public key and params with other party
    void createDecoder(byte[] params); //share encoder params to create decoder
    void message(byte[] msg); //send a message to the object
    Boolean[] getFlags(); //get flags from Security class of other Messenger
    void init(String other); //initialize communication with the object
    void disconnect(); //disconnect from all connections
} 