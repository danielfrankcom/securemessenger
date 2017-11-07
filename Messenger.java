import java.rmi.registry.Registry; 
import java.rmi.registry.LocateRegistry; 
import java.rmi.RemoteException; 
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
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

    private static Boolean[] flags; //this will store the 3 security flags
    //Let's set it manually for now and we can figure out how to modify later
    //[0] - confidentiality
    //[1] - integrity
    //[2] - authentication
      
    /*
    * Initialize the Messenger
    */
    public Messenger() throws Exception{

        comm = new ArrayList<>(); //initialize instance variable

        try{
            registry = LocateRegistry.createRegistry(1099); //try to create registry
        }catch(java.rmi.server.ExportException e){ //if caught, then server already running
            registry = LocateRegistry.getRegistry(); //instead, connect to existing one
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
    public void createPub(byte[] otherPub, CommunicationInterface other) throws Exception{
        secure.createPub(otherPub, other);
    }

    /*
    * Wrapper function for Security class
    * Allows secure pass through of security information
    * @param       byte[] otherPub (public key)
    * @return      void
    */
    public void share(byte[] otherPub, byte[] otherParams) throws Exception{
        secure.share(otherPub, otherParams);
    }

    /*
    * Wrapper function for Security class
    * Allows secure pass through of security information
    * @param       byte[] otherPub (public key)
    * @return      void
    */
    public void createDecoder(byte[] params) throws Exception{
        secure.createDecoder(params);
    }

    /*
    * Receive a message from an external object
    * @param       String message
    * @return      void
    */
    public void message(byte[] msg) throws Exception{
        if(flags[0]){
            cont.addText(comm.get(0).getID() + ": " + secure.decrypt(msg) + "\n");
            System.out.println("flagged");
        }else{
            cont.addText(comm.get(0).getID() + ": " + new String(msg) + "\n");
        }
        //this needs to be expanded later for more connections
        //currently it assumes all messages are from the 1st connection
    }

    /*
    * Sent from controller, user has typed a message to send
    * @param       String message
    * @return      void
    */
    public void typed(String msg) throws Exception{

        cont.addText("you: " + msg + "\n"); //display the message for the user

        for(int i = 0; i < comm.size(); i++){ //sends message to all connections
            if(flags[0]){
                comm.get(i).message(secure.encrypt(msg));
            }else{
                comm.get(i).message(msg.getBytes());
            }
        }

    }

    /*
    * Sent from controller, user has typed a command to run
    * @param       String command
    * @return      void
    */
    public void command(String msg) throws Exception{

        if(msg.equals("exit") || msg.equals("quit") || msg.equals("q")){ //if user wants to quit

            System.exit(0); //quit all threads (close application)

        }else if(msg.contains("connect")){ //if user wants to make a connection

            String temp[] = msg.split(" "); //access the desired connection id
            CommunicationInterface receiver = (CommunicationInterface) registry.lookup(temp[1]); //get from RMI
            receiver.init(id); //initialize communication (add us to receiver's comm array)
            comm.add(receiver); //add to our own
            secure.createSharedSecret(receiver);
            cont.addText("[Connected to: " + temp[1] + "]\n"); //display connection status for user

        }

    }

    /*
    * Initialize communication with sender
    * @param       String senderID
    * @return      void
    */
    public void init(String other) throws Exception{

        CommunicationInterface sender = (CommunicationInterface) registry.lookup(other); //get from RMI
        comm.add(sender); //add sender to our comm array
        cont.addText("[Connected to: " + other + "]\n"); //display connection status for user 

    }

    /*
    * Main method used to run the class
    * @return      void
    */
    public static void main(String[] args) throws Exception{

        Messenger self = new Messenger(); //create a Messenger object

        if(args.length > 0){
            self.setID(args[0]); //set id if custom
        }else{
            self.setID(UUID.randomUUID().toString()); //generate random id if not custom
        }

        String id = self.getID(); //get instance id

        cont = GUI.getInstance(self, "Secure Messenger ("+ id + ")"); //start the GUI and get the controller

        CommunicationInterface stub = (CommunicationInterface) UnicastRemoteObject.exportObject(self, 0); //create RMI compatible stub
        registry.bind(id, stub); //put self in RMI

        cont.addText("[Your id is: " + id + "]\n");
        cont.addText("[Type ':q' to quit or ':connect <id>' to connect to another messenger.]\n");

        secure = new Security(self, id);
        flags = new Boolean[]{false, true, true};
        
    }

}