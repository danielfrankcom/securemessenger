import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

/*
* Server portion of the client/server communication
* Wait for initialization from client
* Receive messages from client and send messages to client
*/
class Server implements Runnable {
   private Thread t;
   private Client cl;
   public BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>(); //store incoming messages here
   public byte[] encodedParams; //need to share the spec
   public int bobLen;
   
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

            byte[] alicePubKeyEnc = queue.take();
            //cl.queue.put("server -> client".getBytes()); //put into client message queue

            KeyFactory bobKeyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);

            PublicKey alicePubKey = bobKeyFac.generatePublic(x509KeySpec);

            /*
            * Bob gets the DH parameters associated with Alice's public key.
            * He must use the same parameters when he generates his own key
            * pair.
            */
            DHParameterSpec dhParamFromAlicePubKey = ((DHPublicKey)alicePubKey).getParams();

            // Bob creates his own DH key pair
            System.out.println("BOB: Generate DH keypair ...");
            KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH");
            bobKpairGen.initialize(dhParamFromAlicePubKey);
            KeyPair bobKpair = bobKpairGen.generateKeyPair();

            // Bob creates and initializes his DH KeyAgreement object
            System.out.println("BOB: Initialization ...");
            KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
            bobKeyAgree.init(bobKpair.getPrivate());

            // Bob encodes his public key, and sends it over to Alice.
            byte[] bobPubKeyEnc = bobKpair.getPublic().getEncoded();
            cl.queue.put(bobPubKeyEnc);
            System.out.println("client sent key");

            System.out.println("BOB: Execute PHASE1 ...");
            bobKeyAgree.doPhase(alicePubKey, true);

           
            queue.take();
            byte[] bobSharedSecret = new byte[cl.aliceLen];
            bobLen = bobKeyAgree.generateSecret(bobSharedSecret, 0);
            
            SecretKeySpec bobAesKey = new SecretKeySpec(bobSharedSecret, 0, 16, "AES");

            Cipher bobCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            bobCipher.init(Cipher.ENCRYPT_MODE, bobAesKey);
            byte[] cleartext = "This is just an example".getBytes();
            byte[] ciphertext = bobCipher.doFinal(cleartext);

            // Retrieve the parameter that was used, and transfer it to Alice in
            // encoded format
            encodedParams = bobCipher.getParameters().getEncoded();
            cl.encodedParams = encodedParams;
            cl.queue.put("done".getBytes());
            cl.queue.put(ciphertext);

        }catch(Exception e){
            System.out.println("Exception caught: " + e.getMessage());
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
}