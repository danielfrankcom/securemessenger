import java.rmi.Remote;

/*
* Defines what methods are accessible over RMI
*/
public interface CommunicationInterface extends Remote { 
    String getID() throws Exception; //get the id string of the object
    void createPub(byte[] otherPub, CommunicationInterface other) throws Exception; //create a public key to otherPub spec
    void share(byte[] otherPub, byte[] otherParams) throws Exception; //share the created public key and params with other party
    void createDecoder(byte[] params) throws Exception; //share encoder params to create decoder
    void message(byte[] msg) throws Exception; //send a message to the object
    void init(String other) throws Exception; //initialize communication with the object
    void disconnect() throws Exception; //disconnect from all connections
} 