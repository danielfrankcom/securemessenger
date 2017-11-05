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

import javafx.application.Application;

import javax.crypto.spec.DHParameterSpec;
import javax.crypto.interfaces.DHPublicKey;

import java.rmi.registry.Registry; 
import java.rmi.registry.LocateRegistry; 
import java.rmi.RemoteException; 
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;

/*
* Server portion of the client/server communication
* Wait for initialization from client
* Receive messages from client and send messages to client
*/
class Messenger implements CommunicationInterface{

    private static Registry registry;
    private ArrayList<CommunicationInterface> comm; //who are we currently communicating with
    private static Controller cont; //GUI controller
    private static String id;
      
    /*
    * Initialize the thread
    **/
    Messenger() throws Exception{
        System.out.println("Server initializing");
        comm = new ArrayList<>();
        try{
            registry = LocateRegistry.createRegistry(1099);
        }catch(java.rmi.server.ExportException e){
            registry = LocateRegistry.getRegistry();
            System.out.println("located");
        }
    }

    public String getID(){
        return id;
    }

    public void setID(String in){
        id = in;
    }


    /*
    * Send message to receiver
    * @param       String message
    * @return      Boolean success
    */
    public Boolean message(String msg) throws Exception{
        cont.addText(comm.get(0).getID() + ": " + msg + "\n"); //this needs to be expanded later for more connections
        return true;
    }

    public void typed(String msg) throws Exception{
        cont.addText("you: " + msg + "\n");
        for(int i = 0; i < comm.size(); i++){
            comm.get(i).message(msg);
        }
    }

    /*
    * Look up sender in registry
    * Initialize communication with sender
    * @param       String sender id
    * @return      void
    */
    public void init(String other) throws Exception{
        CommunicationInterface sender = (CommunicationInterface) registry.lookup(other); 
        comm.add(sender);
        cont.addText("connected to: " + other + "\n"); //msg will be encrypted in future
    }

    /*
    * Main method used to run the class
    * @return      void
    */
    public static void main(String[] args) throws Exception{

        Messenger self = new Messenger();

        cont = new GUI().getInstance(self);

        if(args.length > 0){
            self.setID(args[0]);          
        }else{
            self.setID(UUID.randomUUID().toString());
        }

        String id = self.getID();

        CommunicationInterface stub = (CommunicationInterface) UnicastRemoteObject.exportObject(self, 0);
        registry.bind(id, stub);

        Scanner scanner = new Scanner(System.in);

        Boolean valid = false;
        while(!valid){
            System.out.print("Would you like to connect (c) or listen for connections (l)? ");
            String resp = scanner.nextLine();

            if(resp.equals("c")){
                System.out.print("Enter id: ");
                resp = scanner.nextLine();
                CommunicationInterface receiver = (CommunicationInterface) registry.lookup(resp);
                receiver.init(id);
                self.comm.add(receiver);
                self.cont.addText("Connected to: " + resp + "\n");
                valid = true;
            }else if(resp.equals("l")){
                valid = true;
            }else{
                System.out.println("Please type 'c' or 'l'.");
            }
        }
        
    }

}