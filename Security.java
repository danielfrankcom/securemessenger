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

    private Cipher enCipher; //shared key for encryption
    private Cipher deCipher; //shared key for decryption
    private KeyAgreement keyAgree; //object for access in multiple methods
    private SecretKeySpec aesKey; //instance key for creating ciphers
    private CommunicationInterface other; //messenger that we are communicating with

    private CommunicationInterface self; //our Messenger parent
    private String id; //id of our Messenger parent
    private PrivateKey priv; //private key (not diffie-hellman)
    PublicKey myPub; //public key (not diffie-hellman)

    private static Boolean[] flags; //this will store the 3 security flags
    //Let's set it manually for now and we can figure out how to modify later
    //[0] - confidentiality
    //[1] - integrity
    //[2] - authentication

    /*
    * Initialize the class
    */
    public Security(CommunicationInterface parent, String parentID) throws Exception{

        self = parent; //set instance variables
        id = parentID;

        flags = new Boolean[]{false,false, false};

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

        other.createPub(kPair.getPublic().getEncoded(), self); //send encoded public key to other party

    }

    /*
    * Second step in diffie-hellman protocol
    * Take a public key, and generate our own
    * Send to the other party to create a shared secret
    * @param       byte[] otherPubEnc (other party's public key)
    * @return      void
    */
    public void createPub(byte[] otherPubEnc, CommunicationInterface other) throws Exception{

        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(otherPubEnc); //create a spec to determine other public key
        PublicKey otherPub = keyFac.generatePublic(x509KeySpec); //get other public key

        DHParameterSpec dhParam = ((DHPublicKey)otherPub).getParams(); //create spec to create a similar key

        KeyPairGenerator kPairGen = KeyPairGenerator.getInstance("DH");
        kPairGen.initialize(dhParam);
        KeyPair kPair = kPairGen.generateKeyPair(); //generate keypair based on spec

        KeyAgreement keyAgree = KeyAgreement.getInstance("DH"); //does not need to be externally defined as this step is self-contained
        keyAgree.init(kPair.getPrivate());

        keyAgree.doPhase(otherPub, true);
        byte[] sharedSecret = keyAgree.generateSecret(); //create diffie-hellman secret
        aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");

        enCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        enCipher.init(Cipher.ENCRYPT_MODE, aesKey); //create cipher for encoding
        byte[] params = enCipher.getParameters().getEncoded();

        other.share(kPair.getPublic().getEncoded(), params); //send required information to other party

    }

    /*
    * Third step in diffie-hellman protocol
    * Take a public key and create a shared secret
    * Take cipher params and use secret to create a cipher
    * @param       byte[] otherPubEnc (other party's public key)
    * @return      void
    */
    public void share(byte[] otherPubEnc, byte[] otherParams) throws Exception{
        
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(otherPubEnc); //create a spec to determine other public key
        PublicKey otherPub = keyFac.generatePublic(x509KeySpec); //get other public key

        keyAgree.doPhase(otherPub, true);
        byte[] sharedSecret = keyAgree.generateSecret(); //create diffie-hellman secret
        aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");

        enCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        enCipher.init(Cipher.ENCRYPT_MODE, aesKey); //create cipher for encoding
        byte[] selfParams = enCipher.getParameters().getEncoded();

        other.createDecoder(selfParams); //create decoder for this key
        self.createDecoder(otherParams); //create decoder for other party's key (we do it here to avoid race condition)

    }

    /*
    * Last step in diffie-hellman protocol
    * Take cipher params to create a decoder
    * @param       byte[] params
    * @return      void
    */
    public void createDecoder(byte[] params) throws Exception{

        AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
        aesParams.init(params);
        deCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        deCipher.init(Cipher.DECRYPT_MODE, aesKey, aesParams); //create cipher for decoding

    }

    /*
    * Encrypt text using the cipher
    * @param       String plaintext
    * @return      byte[] ciphertext
    */
    public byte[] encrypt(String plaintext) throws Exception{

        return enCipher.doFinal(plaintext.getBytes());

    }

    /*
    * Utilize flags to create a sendable message
    * @param       String msg (to make sendable)
    * @return      btye[] msg (sendable)
    */
    public byte[] send(String msg) throws Exception{
        if(flags[0]){
            return encrypt(msg);
        }else{
            return msg.getBytes();
        }
    }

    /*
    * Decrypt text using the cipher
    * @param       byte[] ciphertext
    * @return      String plaintext
    */
    public String decrypt(byte[] ciphertext) throws Exception{

        return new String(deCipher.doFinal(ciphertext));

    }

    /*
    * Utilize flags to create a sendable message
    * @param       byte[] msg (to make sendable)
    * @return      String msg (sendable)
    */
    public String receive(byte[] msg) throws Exception{
        if(flags[0]){
            return decrypt(msg);
        }else{
            return new String(msg);
        }
    }
      
    /*
    * Get private key for the current messenger from a file
    * @return      PrivateKey
    */
    private PrivateKey getPrivate() throws Exception {
  
        byte[] keyBytes = Files.readAllBytes(Paths.get("keys/private-" + id + "/private.der"));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(spec);

    }

    /*
    * Get public key for the specified messenger from a file
    * @param       String messenger (id of messenger to get public key for)
    * @return      PublicKey
    */
    private PublicKey getPublic(String messenger) throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get("keys/public/" + messenger + ".der"));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(spec);

    }

}