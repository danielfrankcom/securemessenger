import java.rmi.Remote; 
import java.rmi.RemoteException;  

public interface CommunicationInterface extends Remote { 
    public String getID() throws Exception;
    Boolean message(String msg) throws Exception;
    void init(String other) throws Exception;
} 