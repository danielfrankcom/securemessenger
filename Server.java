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

/*
* Server portion of the client/server communication
* Wait for initialization from client
* Receive messages from client and send messages to client
*/
class Server implements Runnable {
   private Thread t;
   private Client cl;
   private Cipher bobCipher;

   public BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>(); //store messages from client
   public byte[] encodedParams; //need to be the same between communicating parties
   public int bobLen; //this is not useful, but needs to be communicated via network at one point
   
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

        try{
            generateKey(getSharedSecret()); //using diffie-hellman
            byte[] ciphertext = bobCipher.doFinal("secret message goes here".getBytes()); //encrypt message
            cl.queue.put(ciphertext); //put message in client message queue
        }catch(Exception e){
            System.out.println("Exception caught: " + e.getCause().getMessage());
        }

        System.out.println("Server exiting");
    }

    /*
    * Create a thread to run
    * @return      void
    */
    public void start (){
        System.out.println("Server starting");
        if (t == null) {
            t = new Thread (this, "server");
            t.start ();
        }
    }

    /*
    * Get a shared secret between client and server
    * @return      byte[] shared secret
    */
    private byte[] getSharedSecret(){
        try{

            byte[] alicePubKeyEnc = queue.take(); //wait for client to generate a public key

            KeyFactory bobKeyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);
            PublicKey alicePubKey = bobKeyFac.generatePublic(x509KeySpec);

            DHParameterSpec dhParamFromAlicePubKey = ((DHPublicKey)alicePubKey).getParams(); //ensure params are the same for both keys
            KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH");
            bobKpairGen.initialize(dhParamFromAlicePubKey);
            KeyPair bobKpair = bobKpairGen.generateKeyPair();
            
            KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
            bobKeyAgree.init(bobKpair.getPrivate());

            byte[] bobPubKeyEnc = bobKpair.getPublic().getEncoded();
            cl.queue.put(bobPubKeyEnc); //put public key in client message queue
            System.out.println("client sent key");

            bobKeyAgree.doPhase(alicePubKey, true);

            queue.take(); //wait for alice to set key length

            byte[] bobSharedSecret = new byte[cl.aliceLen]; //store shared secret
            bobLen = bobKeyAgree.generateSecret(bobSharedSecret, 0); //set private var

            return bobSharedSecret;

        }catch(Exception e){
            System.out.println("Exception caught: " + e.getCause().getMessage());
        }

        return null;
    }

    /*
    * Use shared secret to make a shared key
    * @return      void
    */
    private void generateKey(byte[] bobSharedSecret){
        try{

            SecretKeySpec bobAesKey = new SecretKeySpec(bobSharedSecret, 0, 16, "AES"); //use shared secret
            bobCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            bobCipher.init(Cipher.ENCRYPT_MODE, bobAesKey); //initialize private variable to store cipher

            encodedParams = bobCipher.getParameters().getEncoded(); //store params to send to client          
            cl.encodedParams = encodedParams; //share params with client
            cl.queue.put("done".getBytes()); //notify client that params have been shared

        }catch(Exception e){
            System.out.println("Exception caught.");
        }
    }

}