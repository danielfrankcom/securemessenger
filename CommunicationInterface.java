import java.rmi.Remote; 
import java.rmi.RemoteException;  

public interface CommunicationInterface extends Remote {  
    Boolean message(String msg) throws RemoteException;
    void init(String id) throws RemoteException;
} 