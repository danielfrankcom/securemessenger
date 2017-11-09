import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

import java.nio.file.Files;
import java.nio.file.Paths;

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

        try{
            priv = getPrivate(); //get from file
            myPub = getPublic(id); //get from file
        }catch(Exception e){
            System.out.println("public/private key read error, maybe user does not exist");
        }

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
    private byte[] encrypt(String plaintext) throws Exception{

        return enCipher.doFinal(plaintext.getBytes());

    }

    /*
    * Utilize flags to create a sendable message
    * @param       String msg (to make sendable)
    * @return      btye[] msg (sendable)
    */
    public byte[][] send(String msg, String receiver) throws Exception{
      testingMethod();
      byte[] msg_ret = msg.getBytes();
      byte[] checksum_ret = new byte[0];
      if(flags[0]){
        msg_ret = encrypt(msg);
      }
      if(flags[1]){
        checksum_ret = encryptCheckSum(receiver, generateCheckSum(msg));
        System.out.println("Encrypted checksum in SEND: "+checksum_ret);
      }
      byte[][] ret = new byte[2][];
      ret[0] = msg_ret;
      ret[1] = checksum_ret;
      return ret;
    }

    /*
    * Decrypt text using the cipher
    * @param       byte[] ciphertext
    * @return      String plaintext
    */
    private String decrypt(byte[] ciphertext) throws Exception{

        return new String(deCipher.doFinal(ciphertext));

    }

    /*
    * Utilize flags to create a sendable message
    * @param       byte[] msg (to make sendable)
    * @return      String msg (sendable)
    */
    public String receive(byte[] msg, byte[] checksum) throws Exception{
      String ret;
      if(flags[0]){
          ret = decrypt(msg);
      }else{
          ret = new String(msg);
      }
      if(flags[1]){
        System.out.println("Encrypted Checksum in receive: "+checksum);
        byte[] decrypted_cs = decryptCheckSum(checksum);
        System.out.println("Decrypted Checksum in receive: "+decrypted_cs);
        System.out.println("Ret in receive: "+ret);
        if(compareCheckSum(decrypted_cs, ret)){
          System.out.println("Debug 000: "+ret);
          return ret;
        }
        else{
          return "Checksum does not match";
        }
      }
      System.out.println("Debug 001: "+ret);
      return ret;
    }

    /*
    * Set flags from Messenger
    * @param       Boolean[] flag
    * @return      void
    */
    public void setFlags(Boolean[] newFlags){

        flags = newFlags;

    }

    /*
    * Set flags from Messenger
    * @param       Boolean[] flag
    * @return      void
    */
    public Boolean[] getFlags(){

        return flags;

    }

    /*
    * Get private key for the current messenger from a file
    * @return      PrivateKey
    */
    private PrivateKey getPrivate() throws Exception{

        byte[] key = Files.readAllBytes(Paths.get("keys/private-" + id + "/private.der"));
        PKCS8EncodedKeySpec PKCS8KeySpec = new PKCS8EncodedKeySpec(key);
        KeyFactory keyFac = KeyFactory.getInstance("RSA");

        return keyFac.generatePrivate(PKCS8KeySpec);

    }

    /*
    * Get public key for the specified messenger from a file
    * @param       String messenger (id of messenger to get public key for)
    * @return      PublicKey
    */
    private PublicKey getPublic(String messenger) throws Exception{

        byte[] key = Files.readAllBytes(Paths.get("keys/public/" + messenger + ".der"));
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(key);
        KeyFactory keyFac = KeyFactory.getInstance("RSA");

        return keyFac.generatePublic(x509KeySpec);

    }
    private byte[] generateCheckSum(String message){
      int m = message.hashCode();
      ByteBuffer bb = ByteBuffer.allocate(4);
      bb.putInt(m);
      return bb.array();
    }

    public byte[] encryptCheckSum(String receiver, byte[] inputData) throws Exception {
        PublicKey key = getPublic(receiver);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.PUBLIC_KEY, key);
        byte[] encryptedBytes = cipher.doFinal(inputData);
        return encryptedBytes;
    }
    public byte[] decryptCheckSum(byte[] checksum) throws Exception {
        PrivateKey key = getPrivate();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.PRIVATE_KEY, key);
        byte[] decryptedBytes = cipher.doFinal(checksum);
        return decryptedBytes;
    }
    private boolean compareCheckSum(byte[] checksum, String message){
      byte[] temp_checksum = generateCheckSum(message);
      if(temp_checksum == checksum){
        return true;
      }
      System.out.println("Temp checksum: "+temp_checksum);
      System.out.println("CHECKSUM: "+checksum);
      return false;
    }
    private void testingMethod() throws Exception{
      System.out.println("-------------------------");
      String message = "Hello";
      byte[] checksum = generateCheckSum(message);
      byte[] encryptedCheckSum = encryptCheckSum("Server",checksum);
      System.out.println("Unencrypted checksum: "+checksum);
      System.out.println("Encrypted checksum: "+encryptedCheckSum);
      byte[] decryptedChecksum = decryptCheckSum(encryptedCheckSum);
      System.out.println("Decrypted checksum: "+decryptedChecksum);
      System.out.println("-------------------------");
    }

}
