import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.rmi.server.ExportException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import java.util.UUID;

/**
* Entry point for peer-to-peer communication, can wait for connection or initialize a connection, receives messages from connected Messengers or can send messages
*/
class Messenger implements CommunicationInterface{

    /**
    * The registry that stores other Messengers
    */
    private static Registry registry;
    /**
    * Who are we currently communicating with
    */
    private CommunicationInterface comm;
    /**
    * GUI controller
    */
    private static Controller cont;
    /**
    * Security object for cryptography
    */
    private static Security secure;
    /**
    * The id of our Messenger
    */
    private static String id;

    /**
    * Initialize the Messenger
    */
    public Messenger(){

        comm = null;

        try{
            registry = LocateRegistry.createRegistry(1099); //try to create registry
        }catch(RemoteException e){ //if caught, then server already running
            try{
                registry = LocateRegistry.getRegistry(); //instead, connect to existing one
            }catch(RemoteException f){
                System.out.println("Error creating registry");
            }
        }

    }

    /**
    * Allows other instances to access our ID
    * @return id string
    */
    public String getID(){

        return id;

    }

    /**
    * Allows main() to set our instance ID
    * @param newID id to replace our current one with
    */
    public void setID(String newID){

        id = newID;

    }

    /**
    * Wrapper function for Security class, allows secure pass through of security information
    * @param otherPub public key that other has created
    * @param other object of sender
    */
    public void createPub(byte[] otherPub, CommunicationInterface other){

        try{
            secure.createPub(otherPub, other);
        }catch(Exception e){
            System.out.println("error in createPub()");
        }

    }

    /**
    * Wrapper function for Security class, allows secure pass through of security information
    * @param otherPub public key that other has created
    * @param otherParams created cipher parameters
    */
    public void share(byte[] otherPub, byte[] otherParams){

        try{
            secure.share(otherPub, otherParams);
        }catch(Exception e){
            System.out.println("error in share()");
        }

    }

    /**
    * Wrapper function for Security class, allows secure pass through of security information
    * @param params created cipher parameters
    */
    public void createDecoder(byte[] params){

        try{
            secure.createDecoder(params);
        }catch(Exception e){
            System.out.println("error in createDecoder()");
        }

    }

    /**
    * Wrapper function for Security class, allows secure pass through of security information
    * @param flags set security flags for the object
    */
    public void setFlags(Boolean[] flags){

        secure.setFlags(flags);

    }

    /**
    * Wrapper function for Security class, allows secure pass through of security information
    * @return the current security flags for the object
    */
    public Boolean[] getFlags(){

        return secure.getFlags();

    }

    /**
    * Receive a message from an external object
    * @param msg message received
    */
    public void message(byte[] msg, byte[] checksum){
        if(secure.getFlags()[2] && !secure.getAuth()){
          cont.addText("[You must authenticate before receiving a message]\n");
          return;
        }
        System.out.println("Received: " + new String(msg));
        try{
            cont.addText(comm.getID() + ": " + secure.receive(msg, checksum) + "\n"); //display received messages
        }catch(Exception e){
            System.out.println("message receiving error");
            e.printStackTrace();
        }

    }

    /**
    * Sent from controller, user has typed a message to send
    * @param msg the message that the user has typed
    */
    public void typed(String msg){
        try{
            byte[][] info = secure.send(msg, comm.getID());
            if(info == null && secure.getFlags()[2]){
              cont.addText("[You must authenticate before sending a message]\n");
              return;
            }
            comm.message(info[0],info[1]); //send a message
        }catch(Exception e){
            //do not print an error, as there is no problem with displaying the message anyway
        }
        cont.addText("you: " + msg + "\n"); //display the message for the user
    }

    /**
    * Sent from controller, user has typed a command to run
    * @param msg command to run
    */
    public void command(String msg){

        if(msg.equals("exit") || msg.equals("quit") || msg.equals("q")){ //if user wants to quit

            System.exit(0); //quit all threads (close application)

        }else if(msg.contains("disconnect")){ //if user wants to disconnect from a connection

            cont.setCheckBoxes(false); //re-enable flag checkboxes
            try{
                comm.disconnect();
                secure.deAuth();
            }catch(RemoteException e){
                System.out.println("error disconnecting");
            }
            comm = null; //overwrite list of other Messengers
            cont.addText("[Disconnected]\n"); //display connection status for user

        }else if(msg.contains("connect")){ //if user wants to make a connection

            String temp[] = msg.split(" "); //access the desired connection id
            if(temp.length == 1){
                cont.addText("[Invalid connection]\n");
                return;
            }
            CommunicationInterface receiver = null;
            if(temp[1].equals(id)){
                cont.addText("[Cannot connect to self]\n");
                return;
            }
            if(comm != null){
                cont.addText("[Please disconnect first]\n");
                return;
            }
            try{
                receiver = (CommunicationInterface) registry.lookup(temp[1]); //get from RMI
            }catch(RemoteException | NotBoundException e){
                cont.addText("[User does not exist]\n");
                return;
            }
            cont.setCheckBoxes(true); //disable flag checkboxes

            Boolean[] otherFlags = null;
            try{
                otherFlags = receiver.getFlags(); //get other Messenger security flags
            }catch(RemoteException e){
                System.out.println("error getting flags of remote object");
            }
            Boolean[] myFlags = secure.getFlags();
            if(!Arrays.equals(myFlags, otherFlags)){ //if flags are not the same
                cont.addText("[Please ensure security flags are the same]\n"); //display connection status for user
                cont.setCheckBoxes(false);
                return; //exit the method early
            }

            try{

                receiver.init(id); //initialize communication (add us to receiver's comm variable)
            }catch(RemoteException e){
                System.out.println("Messenger initialization error");
            }
            comm = receiver; //add to our own
            if(secure.getFlags()[0]){
                try{
                    secure.createSharedSecret(receiver); //ensure encryption is possible if wanted
                }catch(Exception e){
                    System.out.println("error in createSharedSecret()");
                }
            }
            cont.addText("[Connected to " + temp[1] + "]\n"); //display connection status for user
            cont.addText("[Type ':disconnect' to remove connections from other messengers]\n"); //display disconnect prompt
            cont.addText("[Type ':auth <password>' to authenticate yourself]\n"); //display authenticate prompt

        }else if(msg.contains("auth")){ // if user would like to authenticate.
          String temp[] = msg.split(" "); //access the desired password
          if(temp.length == 1){
              cont.addText("[Invalid password]\n");
              return;
          }
          String pass = temp[1];
          try{
            if(secure.authenticate(id,pass)){
              cont.addText("[Authenticated successfully as "+id+"]\n");
            }
            else{
              cont.addText("[Authenticated failed]\n");
            }
          }
          catch(Exception e){
            System.out.println("Error calling authenticate.");
            cont.addText("[Error authenticating]\n");
          }
        }

    }

    /**
    * Initialize communication with sender
    * @param other id of sender
    */
    public void init(String other){

        cont.setCheckBoxes(true); //disable flag checkboxes

        CommunicationInterface sender = null;
        try{
            sender = (CommunicationInterface) registry.lookup(other); //get from RMI
        }catch(RemoteException | NotBoundException e){
            System.out.println("RMI lookup error");
        }
        comm = sender; //save sender
        cont.addText("[Connected to " + other + "]\n"); //display connection status for user
        cont.addText("[Type ':disconnect' to remove connections from other messengers]\n"); //display disconnect prompt
        cont.addText("[Type ':auth <password>' to authenticate yourself]\n"); //display authenticate prompt
    }

    /**
    * Disconnect from a sender
    */
    public void disconnect(){

        cont.setCheckBoxes(false); //re-enable flag checkboxes
        comm = null; //overwrite comm variable
        cont.addText("[Disconnected]\n"); //display connection status for user

    }

    /**
    * Main method used to run the class
    * @param args command line arguments
    */
    public static void main(String[] args){

        Messenger self = new Messenger(); //create a Messenger object

        if(args.length > 0){
            self.setID(args[0]); //set id if custom
        }else{
            self.setID(UUID.randomUUID().toString()); //generate random id if not custom
        }

        String id = self.getID(); //get instance id

        cont = GUI.getInstance(self, "Secure Messenger ("+ id + ")"); //start the GUI and get the controller

        try{
            CommunicationInterface stub = (CommunicationInterface) UnicastRemoteObject.exportObject(self, 0); //create RMI compatible stub
            registry.bind(id, stub); //put self in RMI
        }catch(RemoteException | AlreadyBoundException e){
            System.out.println("RMI binding/lookup error");
        }


        cont.addText("[Your id is: " + id + "]\n");
        cont.addText("[Type ':q' to quit or ':connect <id>' to connect to another messenger]\n");

        try{
            secure = new Security(self, id);
        }catch(Exception e){
            System.out.println("Security initialization error");
        }

    }

}
