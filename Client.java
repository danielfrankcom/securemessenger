import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

/*
* Client portion of the client/server communication
* Initialize communication with the server
* Send messages to server and receive messages from server
*/
class Client implements Runnable{
    private Thread t;
    private Server sv;
    public BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>();
    public byte[] encodedParams; //need to share the spec
    public int aliceLen;
    private Cipher aliceCipher;

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

            //System.out.println(queue.take()); //this waits until there is a message to take          

            generateKey(exchangeSecrets());
     
            byte[] recovered = aliceCipher.doFinal(queue.take());
            System.out.println("Encrypted: " + new String(recovered));

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

    private byte[] exchangeSecrets(){
        byte[] aliceSharedSecret = null;
        try{

            KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
            aliceKpairGen.initialize(2048);
            KeyPair aliceKpair = aliceKpairGen.generateKeyPair();
            // Alice creates and initializes her DH KeyAgreement object
            System.out.println("ALICE: Initialization ...");
            KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
            aliceKeyAgree.init(aliceKpair.getPrivate());
            
            // Alice encodes her public key, and sends it over to Bob.
            byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();
            sv.queue.put(alicePubKeyEnc); //put into server message queue
            System.out.println("client sent key");

            byte[] bobPubKeyEnc = queue.take();

            KeyFactory aliceKeyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
            x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
            PublicKey bobPubKey = aliceKeyFac.generatePublic(x509KeySpec);
            System.out.println("ALICE: Execute PHASE1 ...");
            aliceKeyAgree.doPhase(bobPubKey, true);

            aliceSharedSecret = aliceKeyAgree.generateSecret();
            aliceLen = aliceSharedSecret.length;
            sv.queue.put("done".getBytes());
            queue.take();
        }catch(Exception e){
            System.out.println("Exception caught.");
        }
        return aliceSharedSecret;
    }

    private void generateKey(byte[] aliceSharedSecret){

        try{
            SecretKeySpec aliceAesKey = new SecretKeySpec(aliceSharedSecret, 0, 16, "AES");
            AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
            aesParams.init(encodedParams);
            aliceCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aliceCipher.init(Cipher.DECRYPT_MODE, aliceAesKey, aesParams);
        }catch(Exception e){
            System.out.println("Exception caught.");
        }

    }

}