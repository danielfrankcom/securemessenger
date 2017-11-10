import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
* Handles security related tasks on behalf of Messenger, interacts with the javax.crypto and java.security libraries
*/
class Security{

    /**
    * Shared key for encryption
    */
    private Cipher enCipher;
    /**
    * Shared key for decryption
    */
    private Cipher deCipher;
    /**
    * Object for access in multiple methods
    */
    private KeyAgreement keyAgree;
    /**
    * Instance key for creating ciphers
    */
    private SecretKeySpec aesKey;
    /**
    * Messenger that we are communicating with
    */
    private CommunicationInterface other;

    /*
    * Our Messenger parent
    */
    private CommunicationInterface self;
    /*
    * Id of our Messenger parent
    */
    private String id;
    /*
    * Private key (not diffie-hellman)
    */
    private PrivateKey priv;
    /*
    * Public key (not diffie-hellman)
    */
    PublicKey myPub;

    /*
    * Stores the 3 security flags in the following order: confidentiality, integrity, authentication
    */
    private static Boolean[] flags;

    /*
    * Stores the boolean of whether the user has authenticated successfully as the id of our messenger parent.
    */
    private Boolean authenticated = false;
    /**
    * Initialize Security
    * @param parent object of caller
    * @param parentID id of caller
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

    /**
    * First step in diffie-hellman protocol, generate public key and send to other party
    * @param obj party to send to
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

    /**
    * Second step in diffie-hellman protocol, take a public key and generate our own, send to the other party to create a shared secret
    * @param otherPubEnc other party's public key
    * @param other object of other party
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

    /**
    * Third step in diffie-hellman protocol, take a public key and create a shared secret, take cipher params and use secret to create a cipher
    * @param otherPubEnc other party's public key
    * @param otherParams other party's cypher paremeters
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

    /**
    * Last step in diffie-hellman protocol, take cipher params to create a decoder
    * @param params cipher parameters
    */
    public void createDecoder(byte[] params) throws Exception{

        AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
        aesParams.init(params);
        deCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        deCipher.init(Cipher.DECRYPT_MODE, aesKey, aesParams); //create cipher for decoding

    }

    /**
    * Encrypt text using the cipher
    * @param plaintext readable version of message
    * @return unreadable version of message
    */
    private byte[] encrypt(String plaintext) throws Exception{

        return enCipher.doFinal(plaintext.getBytes());

    }

    /**
    * Utilize flags to create a sendable message
    * @param msg message to make sendable
    * @return sendable message
    */
    public byte[][] send(String msg, String receiver) throws Exception{
      byte[] msg_ret = msg.getBytes();
      byte[] checksum_ret = new byte[0];
      if(flags[0]){
        msg_ret = encrypt(msg);
      }
      if(flags[1]){
        checksum_ret = encryptCheckSum(receiver, generateCheckSum(msg));
      }
      if(flags[2]){
        if(!authenticated){
          System.out.println(id+" attempted to send a message while unauthenticated.");
          return null;
        }
      }
      byte[][] ret = new byte[2][];
      ret[0] = msg_ret;
      ret[1] = checksum_ret;
      return ret;
    }

    /**
    * Decrypt text using the cipher
    * @param ciphertext unreadable version of message
    * @return readable version of message
    */
    private String decrypt(byte[] ciphertext) throws Exception{

        return new String(deCipher.doFinal(ciphertext));

    }

    /**
    * Utilize flags to create a sendable message
    * @param msg message to make readable
    * @return readable message
    */
    public String receive(byte[] msg, byte[] checksum) throws Exception{
      String ret;
      if(flags[0]){
          ret = decrypt(msg);
      }else{
          ret = new String(msg);
      }
      if(flags[1]){
        byte[] decrypted_cs = decryptCheckSum(checksum);
        if(compareCheckSum(decrypted_cs, ret)){
          return ret;
        }else{
          return "Checksum does not match";
        }
      }
      if(flags[2]){
        if(!authenticated){
          System.out.println(id+" did not receive message due to not being authenticated.");
          return null;
        }
      }
      return ret;
    }

    /**
    * Set security flags from Messenger
    * @param newFlags security flags to set
    */
    public void setFlags(Boolean[] newFlags){

        flags = newFlags;

    }

    /**
    * Get security flags and return them
    * @return current security flags
    */
    public Boolean[] getFlags(){

        return flags;

    }

    /**
    * Get private key for the current messenger from a file
    * @return our private key
    */
    private PrivateKey getPrivate() throws Exception{

        byte[] key = Files.readAllBytes(Paths.get("keys/private-" + id + "/private.der"));
        PKCS8EncodedKeySpec PKCS8KeySpec = new PKCS8EncodedKeySpec(key);
        KeyFactory keyFac = KeyFactory.getInstance("RSA");

        return keyFac.generatePrivate(PKCS8KeySpec);

    }

    /**
    * Get public key for the specified messenger from a file
    * @param messenger id of messenger to get public key for
    * @return public key of the messenger
    */
    private PublicKey getPublic(String messenger) throws Exception{

        byte[] key = Files.readAllBytes(Paths.get("keys/public/" + messenger + ".der"));
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(key);
        KeyFactory keyFac = KeyFactory.getInstance("RSA");

        return keyFac.generatePublic(x509KeySpec);

    }

    /**
    * Create a checksum for a specific message
    * @param message message to generate a checksum for
    * @return created checksum
    */
    private byte[] generateCheckSum(String message){
        int m = message.hashCode();
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(m);
        System.out.println("Generated checksum: "+toHexString(bb.array()));
        return bb.array();
    }

    /**
    * Encrypt a given checksum
    * @param receiver id of Messenger to receive
    * @param inputData data to encrypt
    * @return encrypted checksum
    */
    private byte[] encryptCheckSum(String receiver, byte[] inputData) throws Exception {
        PublicKey key = getPublic(receiver);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.PUBLIC_KEY, key);
        byte[] encryptedBytes = cipher.doFinal(inputData);
        return encryptedBytes;
    }

    /**
    * Decrypt a given checksum
    * @param checksum checksum to decrypt
    * @return decrypted checksum
    */
    private byte[] decryptCheckSum(byte[] checksum) throws Exception {

        PrivateKey key = getPrivate();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.PRIVATE_KEY, key);
        byte[] decryptedBytes = cipher.doFinal(checksum);
        return decryptedBytes;

    }

    /**
    * Compare two checksums
    * @param checksum checksum to check
    * @param message message to confirm same as checksum
    * @return true if same, false otherwise
    */
    private boolean compareCheckSum(byte[] checksum, String message){
        byte[] temp_checksum = generateCheckSum(message);
        if(Arrays.equals(temp_checksum, checksum)){
            System.out.println("Checksum matches: "+toHexString(temp_checksum));
            return true;
        }
        return false;

    }

    /**
    * Attempts to authenticate a user
    * @param id id of user to authenticate
    * @param pass attempted password to authenticate
    * @return true if successful, false if not
    */
    public boolean authenticate(String id, String pass)throws Exception{
      byte[] bom = pass.getBytes("UTF-8");
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hash = md.digest(bom);
      String attempted_hash = toHexString(hash);
      String stored_hash = getStoredHash(id);
      if(attempted_hash.equals(stored_hash)){
        authenticated = true;
        System.out.println(id+" authenticated successfully.");
        return true;
      }else{
        System.out.println("Attempted password hash: "+attempted_hash);
        System.out.println("Stored password hash: "+stored_hash);
        System.out.println(id+" failed to authenticate.");
        return false;
      }
    }

    /**
    * Gets the stored password hash for id
    * @param id id of user to get password hash of
    * @return password hash if found, "" if not
    */
    private String getStoredHash(String id){
      String line;
      try {
        BufferedReader bufferreader = new BufferedReader(new FileReader("users"));
        line = bufferreader.readLine();
        while (line != null) {
            String[] el = line.split(":");
            if(el[0].equals(id)){
              return el[1];
            }
            line = bufferreader.readLine();
        }
      } catch (FileNotFoundException ex) {
        ex.printStackTrace();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      return "";
    }

    /**
    * Converts MD5 byte array to a string
    * @param bytes byte array to convert
    * @return Base 16 string
    */
    private String toHexString(byte[] bytes) {
      StringBuilder hexString = new StringBuilder();
      for (int i = 0; i < bytes.length; i++) {
          String hex = Integer.toHexString(0xFF & bytes[i]);
          if (hex.length() == 1) {
              hexString.append('0');
          }
          hexString.append(hex);
      }
      return hexString.toString();
    }

    /**
    * Deauthorizes id
    */
    public void deAuth(){
      authenticated = false;
    }

    /**
    * Checks to see if id is authenticated
    * @return true or false
    */
    public boolean getAuth(){
      return authenticated;
    }

}
