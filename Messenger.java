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

/*
* Entry point for peer-to-peer communication
* Can wait for connection or initialize a connection
* Receives messages from connected Messengers or can send messages
*/
class Messenger implements CommunicationInterface{

    private static Registry registry; //the registry that stores other Messengers
    private ArrayList<CommunicationInterface> comm; //who are we currently communicating with
    private static Controller cont; //GUI controller
    private static Security secure; //Security object for cryptography
    private static String id; //the id of our Messenger

    /*
    * Initialize the Messenger
    */
    public Messenger(){

        comm = new ArrayList<>(); //initialize instance variable

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

    /*
    * Allows other instances to access our ID
    * @return      String id
    */
    public String getID(){

        return id;

    }

    /*
    * Allows main() to set our instance ID
    * @param      String id
    * @return     void
    */
    public void setID(String newID){

        id = newID;

    }

    /*
    * Wrapper function for Security class
    * Allows secure pass through of security information
    * @param       byte[] otherPub (public key)
    * @return      void
    */
    public void createPub(byte[] otherPub, CommunicationInterface other){

        try{
            secure.createPub(otherPub, other);
        }catch(Exception e){
            System.out.println("error in createPub()");
        }

    }

    /*
    * Wrapper function for Security class
    * Allows secure pass through of security information
    * @param       byte[] otherPub (public key)
    * @return      void
    */
    public void share(byte[] otherPub, byte[] otherParams){

        try{
            secure.share(otherPub, otherParams);
        }catch(Exception e){
            System.out.println("error in share()");
        }

    }

    /*
    * Wrapper function for Security class
    * Allows secure pass through of security information
    * @param       byte[] otherPub (public key)
    * @return      void
    */
    public void createDecoder(byte[] params){

        try{
            secure.createDecoder(params);
        }catch(Exception e){
            System.out.println("error in createDecoder()");
        }

    }

    /*
    * Wrapper function for Security class
    * Allows secure pass through of security information
    * @param       Boolean[] flags
    * @return      void
    */
    public void setFlags(Boolean[] flags){

        secure.setFlags(flags);

    }

    /*
    * Wrapper function for Security class
    * Allows secure pass through of security information
    * @param       Boolean[] flags
    * @return      void
    */
    public Boolean[] getFlags(){

        return secure.getFlags();

    }

    /*
    * Receive a message from an external object
    * @param       String message
    * @return      void
    */
    public void message(byte[] msg, byte[] checksum){

        try{
            cont.addText(comm.get(0).getID() + ": " + secure.receive(msg, checksum) + "\n"); //display received messages
        }catch(Exception e){
            System.out.println("message receiving error");
        }
        //this could be expanded outside of the scope of the assignment
        //currently it assumes all messages are from the 1st connection

    }

    /*
    * Sent from controller, user has typed a message to send
    * @param       String message
    * @return      void
    */
    public void typed(String msg){

        cont.addText("you: " + msg + "\n"); //display the message for the user
        for(int i = 0; i < comm.size(); i++){ //sends message to all connections
            try{
              byte[][] info = secure.send(msg, comm.get(0).getID());
              comm.get(i).message(info[0],info[1]); //send a message
            }catch(Exception e){
                System.out.println("message sending error");
            }
        }

    }

    /*
    * Sent from controller, user has typed a command to run
    * @param       String command
    * @return      void
    */
    public void command(String msg){

        if(msg.equals("exit") || msg.equals("quit") || msg.equals("q")){ //if user wants to quit

            System.exit(0); //quit all threads (close application)

        }else if(msg.contains("disconnect")){ //if user wants to disconnect from a connection

            cont.setCheckBoxes(false); //re-enable flag checkboxes
            for(int i = 0; i < comm.size(); i++){ //disconnect from all connections
                try{
                    comm.get(i).disconnect();
                }catch(RemoteException e){
                    System.out.println("error disconnecting");
                }
            }
            comm = new ArrayList<>(); //overwrite list of other Messengers
            cont.addText("[Disconnected]\n"); //display connection status for user

        }else if(msg.contains("connect")){ //if user wants to make a connection

            String temp[] = msg.split(" "); //access the desired connection id
            CommunicationInterface receiver = null;
            try{
                receiver = (CommunicationInterface) registry.lookup(temp[1]); //get from RMI
            }catch(RemoteException | NotBoundException e){
                System.out.println("RMI lookup error");
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
                receiver.init(id); //initialize communication (add us to receiver's comm array)
            }catch(RemoteException e){
                System.out.println("Messenger initialization error");
            }
            comm.add(receiver); //add to our own
            if(secure.getFlags()[0]){
                try{
                    secure.createSharedSecret(receiver); //ensure encryption is possible if wanted
                }catch(Exception e){
                    System.out.println("error in createSharedSecret()");
                }
            }
            cont.addText("[Connected to " + temp[1] + "]\n"); //display connection status for user
            cont.addText("[Type ':disconnect' to remove connections from other messengers]\n"); //display disconnect prompt

        }

    }

    /*
    * Initialize communication with sender
    * @param       String senderID
    * @return      void
    */
    public void init(String other){

        cont.setCheckBoxes(true); //disable flag checkboxes

        CommunicationInterface sender = null;
        try{
            sender = (CommunicationInterface) registry.lookup(other); //get from RMI
        }catch(RemoteException | NotBoundException e){
            System.out.println("RMI lookup error");
        }
        comm.add(sender); //add sender to our comm array
        cont.addText("[Connected to " + other + "]\n"); //display connection status for user
        cont.addText("[Type ':disconnect' to remove connections from other messengers]\n"); //display disconnect prompt

    }

    /*
    * Disconnect from a sender
    * @return      void
    */
    public void disconnect(){

        cont.setCheckBoxes(false); //re-enable flag checkboxes
        comm = new ArrayList<>(); //overwrite list of other Messengers
        cont.addText("[Disconnected]\n"); //display connection status for user

    }

    /*
    * Main method used to run the class
    * @return      void
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
