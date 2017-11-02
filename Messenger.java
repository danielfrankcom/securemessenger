import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.interfaces.DHPublicKey;

import java.rmi.registry.Registry; 
import java.rmi.registry.LocateRegistry; 
import java.rmi.RemoteException; 
import java.rmi.server.UnicastRemoteObject; 
import java.util.UUID;

/*
* Server portion of the client/server communication
* Wait for initialization from client
* Receive messages from client and send messages to client
*/
class Messenger implements CommunicationInterface{
    

    /*
    * Initialize the thread
    **/
    Messenger() {
        System.out.println("Server initializing");
    }

    /*
    * Send message to receiver
    * @param       String message
    * @return      Boolean success
    */
    public Boolean message(String msg){
        System.out.println(msg);
        return true;
    }

    /*
    * Look up sender in registry
    * Initialize communication with sender
    * @param       String sender id
    * @return      void
    */
    public void init(String id){
        CommunicationInterface server = (CommunicationInterface) registry.lookup(id); 
        server.message("public message"); //msg will be encrypted in future
    }

    /*
    * Main method used to run the class
    * @return      void
    */
    public static void main(String[] args) throws Exception{
        
        System.out.println("running");
        Messenger self = new Messenger();

        Boolean isServer = false;
        String id;

        if(args.length > 0){
            if(args[0].equals("server")){
                isServer = true;
            }
            id = args[0];          
        }else{
            id = UUID.randomUUID().toString();
        }

        System.out.println(id);
        CommunicationInterface stub = (CommunicationInterface) UnicastRemoteObject.exportObject(self, 0); 
        Registry registry = LocateRegistry.getRegistry();
        registry.bind(id, stub);

        if(!isServer){
            
        }else{
            self.message("self send");
        }
        
    }

}