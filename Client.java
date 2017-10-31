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
    private Cipher aliceCipher;

    public BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>(); //store messages from server
    public byte[] encodedParams; //need to be the same between communicating parties
    public int aliceLen; //this is not useful, but needs to be communicated via network at one point

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
            generateKey(getSharedSecret());
            String message = new String(aliceCipher.doFinal(queue.take())); //get from queue and decrypt message
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

            KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
            aliceKpairGen.initialize(2048);
            KeyPair aliceKpair = aliceKpairGen.generateKeyPair();
            KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
            aliceKeyAgree.init(aliceKpair.getPrivate());
            
            byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();
            sv.queue.put(alicePubKeyEnc); //put public key into server message queue
            System.out.println("client sent key");

            byte[] bobPubKeyEnc = queue.take(); //wait for server to generate a public key

            KeyFactory aliceKeyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
            x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
            PublicKey bobPubKey = aliceKeyFac.generatePublic(x509KeySpec);
            aliceKeyAgree.doPhase(bobPubKey, true);

            byte[] aliceSharedSecret = aliceKeyAgree.generateSecret(); //store shared secret
            aliceLen = aliceSharedSecret.length; //set public var for use by server (or later send over network)
            sv.queue.put("done".getBytes()); //notify server that this var has been set (or later not necessary)
            queue.take(); //wait for server to process
            return aliceSharedSecret;

        }catch(Exception e){
            System.out.println("Exception caught: " + e.getCause().getMessage());
        }

        return null;
    }

    /*
    * Use shared secret to make a shared key
    * @return      void
    */
    private void generateKey(byte[] aliceSharedSecret){
        try{

            SecretKeySpec aliceAesKey = new SecretKeySpec(aliceSharedSecret, 0, 16, "AES"); //use shared secret
            AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
            aesParams.init(encodedParams);
            aliceCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aliceCipher.init(Cipher.DECRYPT_MODE, aliceAesKey, aesParams); //initialize private variable to store cipher

        }catch(Exception e){
            System.out.println("Exception caught.");
        }
    }

}