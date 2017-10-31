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

/*
* Client portion of the client/server communication
* Initialize communication with the server
* Send messages to server and receive messages from server
*/
class Client implements Runnable{

    private Thread t;
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

        try{         
            generateKey(getSharedSecret()); //using diffie-hellman
            String message = new String(cipher.doFinal(queue.take())); //get from queue and decrypt message
            System.out.println("Encrypted: " + message);
        }catch(Exception e){
            System.out.println("Exception caught: " + e.getCause().getMessage());
        }

        System.out.println("Client exiting");
    }
    
    /*
    * Create a thread to run
    * @return      void
    */
    public void start (){
        System.out.println("Client starting");
        if (t == null) {
            t = new Thread (this, "client");
            t.start ();
        }
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

            byte[] bobPubKeyEnc = queue.take(); //wait for server to generate a public key

            KeyFactory keyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
            x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
            PublicKey bobPubKey = keyFac.generatePublic(x509KeySpec);
            keyAgree.doPhase(bobPubKey, true);

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

}