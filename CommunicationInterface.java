import java.rmi.Remote;
import java.rmi.RemoteException;

/**
* Defines what methods are accessible over RMI
*/
public interface CommunicationInterface extends Remote {

    /**
    * Get the id string of the object
    */
    String getID() throws RemoteException;
    /**
    * Create a public key to otherPub spec
    */
    void createPub(byte[] otherPub, CommunicationInterface other) throws RemoteException;
    /**
    * Share the created public key and params with other party
    */
    void share(byte[] otherPub, byte[] otherParams) throws RemoteException;
    /**
    * Share encoder params to create decoder
    */
    void createDecoder(byte[] params) throws RemoteException;
    /**
    * Send a message to the object
    */
    void message(byte[] msg, byte[] checksum) throws RemoteException;
    /**
    * Get flags from Security class of other Messenger
    */
    Boolean[] getFlags() throws RemoteException;
    /**
    * Initialize communication with the object
    */
    void init(String other) throws RemoteException;
    /**
    * Disconnect from all connections
    */
    void disconnect() throws RemoteException;
    
}