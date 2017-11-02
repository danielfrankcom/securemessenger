import java.rmi.Remote; 
import java.rmi.RemoteException;  

public interface CommunicationInterface extends Remote{  
   void printMsg() throws RemoteException;
} 