import java.rmi.Remote;
import java.rmi.RemoteException;

/*
* Defines what methods are accessible over RMI
*/
public interface CommunicationInterface extends Remote { 

    String getID() throws RemoteException; //get the id string of the object
    void createPub(byte[] otherPub, CommunicationInterface other) throws RemoteException; //create a public key to otherPub spec
    void share(byte[] otherPub, byte[] otherParams) throws RemoteException; //share the created public key and params with other party
    void createDecoder(byte[] params) throws RemoteException; //share encoder params to create decoder
    void message(byte[] msg) throws RemoteException; //send a message to the object
    Boolean[] getFlags() throws RemoteException; //get flags from Security class of other Messenger
    void init(String other) throws RemoteException; //initialize communication with the object
    void disconnect() throws RemoteException; //disconnect from all connections
    
} 