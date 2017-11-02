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

/*
* Server portion of the client/server communication
* Wait for initialization from client
* Receive messages from client and send messages to client
*/
class Server implements CommunicationInterface{
   private Client cl;
   private Cipher cipher;

   public BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>(); //store messages from client
   public byte[] encodedParams; //need to be the same between communicating parties
   public int len; //this is not useful, but needs to be communicated via network at one point
   
    /*
    * Initialize the thread
    */
    Server() {
        System.out.println("Server initializing");
    }
    
    /*
    * Locally assign the client thread
    * @return      void
    */
    public void assign(Client client){
        cl = client;
    }
   
    /*
    * Run the thread code
    * @return      void
    */
    public void run(){
        System.out.println("Server running");

        generateKey(getSharedSecret()); //using diffie-hellman

        sendMessage("secret message goes here"); //send message

        System.out.println("Server exiting");
    }

    /*
    * Override communication method print()
    * @return      void
    */
    public String print(){
        System.out.println("This was successfully printed by the client");
        return "secret text";
    }

    /*
    * Get a shared secret between client and server
    * @return      byte[] shared secret
    */
    private byte[] getSharedSecret(){
        try{

            byte[] clientPubKeyEnc = queue.take(); //wait for client to generate a public key

            KeyFactory keyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientPubKeyEnc);
            PublicKey clientPubKey = keyFac.generatePublic(x509KeySpec);

            DHParameterSpec dhParamFromClientPubKey = ((DHPublicKey)clientPubKey).getParams(); //ensure params are the same for both keys
            KeyPairGenerator kPairGen = KeyPairGenerator.getInstance("DH");
            kPairGen.initialize(dhParamFromClientPubKey);
            KeyPair kPair = kPairGen.generateKeyPair();
            
            KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
            keyAgree.init(kPair.getPrivate());

            byte[] pubKeyEnc = kPair.getPublic().getEncoded();
            cl.queue.put(pubKeyEnc); //put public key in client message queue
            System.out.println("client sent key");

            keyAgree.doPhase(clientPubKey, true);

            queue.take(); //wait for client to set key length

            byte[] sharedSecret = new byte[cl.len]; //store shared secret
            len = keyAgree.generateSecret(sharedSecret, 0); //set private var

            return sharedSecret;

        }catch(Exception e){
            System.out.println("Exception caught: " + e.getCause().getMessage());
        }

        return null;
    }

    /*
    * Use shared secret to make a shared key
    * @param       byte[] sharedSecret
    * @return      void
    */
    private void generateKey(byte[] sharedSecret){
        try{

            SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES"); //use shared secret
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey); //initialize private variable to store cipher

            encodedParams = cipher.getParameters().getEncoded(); //store params to send to client          
            cl.encodedParams = encodedParams; //share params with client
            cl.queue.put("done".getBytes()); //notify client that params have been shared

        }catch(Exception e){
            System.out.println("Exception caught.");
        }
    }

    /*
    * Wait for message in queue and decrypt
    * @return      String message
    */
    private String getMessage(){
        try{          
            String message = new String(cipher.doFinal(queue.take())); //get from queue and decrypt message
            return message;
        }catch(Exception e){
            System.out.println("Exception caught: " + e.getCause().getMessage());
        }
        return null;
    }

    /*
    * Send message to client message queue
    * @param       String message
    * @return      void
    */
    private void sendMessage(String message){
        try{          
            byte[] ciphertext = cipher.doFinal(message.getBytes()); //encrypt message
            cl.queue.put(ciphertext); //put message in client message queue
        }catch(Exception e){
            System.out.println("Exception caught: " + e.getCause().getMessage());
        }
    }

    /*
    * Main method used to run the class
    * @return      void
    */
    public static void main(String[] args) throws Exception{
        System.out.println("server");
        Server sv = new Server(); 
         
        CommunicationInterface stub = (CommunicationInterface) UnicastRemoteObject.exportObject(sv, 0);   
        Registry registry = LocateRegistry.getRegistry(); 
        
        registry.bind("Server", stub);  
    }

}