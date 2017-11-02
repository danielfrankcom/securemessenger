import java.rmi.Remote; 
import java.rmi.RemoteException;  

public interface CommunicationInterface extends Remote {  
    Boolean message(String msg) throws Exception;
    void init(String id) throws Exception;
} 