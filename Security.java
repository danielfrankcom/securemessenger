import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;

/*
* Handles security related tasks on behalf of Messenger
* Interacts with the javax.crypto and java.security libraries
*/
class Security{

    private Cipher cipher; //shared key for encryption/decryption
    private KeyAgreement keyAgree; //object for access in multiple methods
    private CommunicationInterface other; //messenger that we are communicating with

    private String id; //string id of our Messenger parent
    PrivateKey priv; //private key (not diffie-hellman)
    PublicKey myPub; //public key (not diffie-hellman)

    /*
    * Initialize the class
    */
    public Security(String parentID) throws Exception{

        id = parentID; //set instance variable
        priv = getPrivate(); //get from file
        myPub = getPublic(id); //get from file

    }

    /*
    * First step in diffie-hellman protocol
    * Generate public key and send to other party
    * @param       CommunicationInterface obj (party to send to)
    * @return      void
    */
    public void createSharedSecret(CommunicationInterface obj) throws Exception{

        other = obj; //set instance var for use by other functions

        KeyPairGenerator kPairGen = KeyPairGenerator.getInstance("DH");
        kPairGen.initialize(2048);
        KeyPair kPair = kPairGen.generateKeyPair(); //generate diffie-hellman public/private pair
        
        keyAgree = KeyAgreement.getInstance("DH"); //init KeyAgreement instance for use in generating secret
        keyAgree.init(kPair.getPrivate());

        other.createPub(kPair.getPublic().getEncoded()); //send encoded public key to other party

    }

    /*
    * Second step in diffie-hellman protocol
    * Take a public key, and generate our own
    * Send to the other party to create a shared secret
    * @param       byte[] otherPubEnc (other party's public key)
    * @return      void
    */
    public void createPub(byte[] otherPubEnc) throws Exception{

        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(otherPubEnc); //create a spec to determine other public key
        PublicKey otherPub = keyFac.generatePublic(x509KeySpec); //get other public key

        DHParameterSpec dhParam = ((DHPublicKey)otherPub).getParams(); //create spec to create a similar key

        KeyPairGenerator kPairGen = KeyPairGenerator.getInstance("DH");
        kPairGen.initialize(dhParam);
        KeyPair kPair = kPairGen.generateKeyPair(); //generate keypair based on spec

        KeyAgreement keyAgree = KeyAgreement.getInstance("DH"); //does not need to be externally defined as this step is self-contained
        keyAgree.init(kPair.getPrivate());

        other.sharePub(kPair.getPublic().getEncoded()); //send encoded public key to other party

        keyAgree.doPhase(otherPub, true);
        byte[] sharedSecret = keyAgree.generateSecret(); //create diffie-hellman secret

    }

    /*
    * Third step in diffie-hellman protocol
    * @param       byte[] otherPubEnc (other party's public key)
    * @return      void
    */
    public void sharePub(byte[] otherPubEnc) throws Exception{
        
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(otherPubEnc); //create a spec to determine other public key
        PublicKey otherPub = keyFac.generatePublic(x509KeySpec); //get other public key

        keyAgree.doPhase(otherPub, true);
        byte[] sharedSecret = keyAgree.generateSecret(); //create diffie-hellman secret

    }

    /*
    * Sent from controller, user has typed a command to run
    * @param       String command
    * @return      void
    */
    public void generateCipher(){

        cipher = null;

    }
      
    /*
    * Sent from controller, user has typed a command to run
    * @param       String command
    * @return      void
    */
    private PrivateKey getPrivate() throws Exception {
  
        byte[] keyBytes = Files.readAllBytes(Paths.get("keys/private-" + id + "/private.der"));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(spec);

    }

    /*
    * Sent from controller, user has typed a command to run
    * @param       String command
    * @return      void
    */
    private PublicKey getPublic(String messenger) throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get("keys/public/" + messenger + ".der"));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(spec);

    }

}