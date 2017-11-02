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

import java.rmi.registry.LocateRegistry; 
import java.rmi.registry.Registry; 

/*
* Client portion of the client/server communication
* Initialize communication with the server
* Send messages to server and receive messages from server
*/
class Client{

    private Server sv;
    private Cipher cipher;

    public BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>(); //store messages from server
    public byte[] encodedParams; //need to be the same between communicating parties
    public int len; //this is not useful, but needs to be communicated via network at one point

    /*
    * Initialize the thread
    */
    Client() {
        System.out.println("Client initializing");
    }

    /*
    * Locally assign the server thread
    * @return      void
    */
    public void assign(Server server){
        sv = server;
    }
   
    /*
    * Run the thread code
    * @return      void
    */
    public void run(){
        System.out.println("Client running");

        generateKey(getSharedSecret()); //using diffie-hellman

        System.out.println("Encrypted: " + getMessage()); //get message

        System.out.println("Client exiting");
    }

    /*
    * Get a shared secret between client and server
    * @return      byte[] shared secret
    */
    private byte[] getSharedSecret(){
        try{

            KeyPairGenerator kpairGen = KeyPairGenerator.getInstance("DH");
            kpairGen.initialize(2048);
            KeyPair kpair = kpairGen.generateKeyPair();
            KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
            keyAgree.init(kpair.getPrivate());
            
            byte[] pubKeyEnc = kpair.getPublic().getEncoded();
            sv.queue.put(pubKeyEnc); //put public key into server message queue
            System.out.println("client sent key");

            byte[] clientPubKeyEnc = queue.take(); //wait for server to generate a public key

            KeyFactory keyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientPubKeyEnc);
            x509KeySpec = new X509EncodedKeySpec(clientPubKeyEnc);
            PublicKey clientPubKey = keyFac.generatePublic(x509KeySpec);
            keyAgree.doPhase(clientPubKey, true);

            byte[] sharedSecret = keyAgree.generateSecret(); //store shared secret
            len = sharedSecret.length; //set public var for use by server (or later send over network)
            sv.queue.put("done".getBytes()); //notify server that this var has been set (or later not necessary)
            
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

            queue.take(); //wait for server to notify that params have been declared

            SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES"); //use shared secret
            AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
            aesParams.init(encodedParams);
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, aesParams); //initialize private variable to store cipher

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
    * Send message to server message queue
    * @param       String message
    * @return      void
    */
    private void sendMessage(String message){
        try{          
            byte[] ciphertext = cipher.doFinal(message.getBytes()); //encrypt message
            sv.queue.put(ciphertext); //put message in server message queue
        }catch(Exception e){
            System.out.println("Exception caught: " + e.getCause().getMessage());
        }
    }
    
    /*
    * Main method used to run the class
    * @return      void
    */
    public static void main(String[] args) throws Exception{
        System.out.println("client");
        
        Registry registry = LocateRegistry.getRegistry(null); 
        
        // Looking up the registry for the remote object 
        CommunicationInterface stub = (CommunicationInterface) registry.lookup("Server"); 

        // Calling the remote method using the obtained object 
        stub.print(); 
    }

}