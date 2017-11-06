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
    private KeyAgreement keyAgree;
    private CommunicationInterface other;

    private String id;
    PrivateKey priv;
    PublicKey myPub;

    /*
    * Initialize the class
    */
    public Security(String parentID) throws Exception{

        id = parentID;
        priv = getPrivate();
        myPub = getPublic(id);

    }

    public void createSharedSecret(CommunicationInterface obj) throws Exception{

        other = obj; //set instance var for use by other functions

        KeyPairGenerator kPairGen = KeyPairGenerator.getInstance("DH");
        kPairGen.initialize(2048);
        KeyPair kPair = kPairGen.generateKeyPair(); //generate diffie-hellman public/private pair
        
        keyAgree = KeyAgreement.getInstance("DH");
        keyAgree.init(kPair.getPrivate());

        other.createPub(kPair.getPublic().getEncoded()); //send encoded public key to other party

    }

    public void createPub(byte[] otherPubEnc) throws Exception{

        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(otherPubEnc);

        PublicKey otherPub = keyFac.generatePublic(x509KeySpec);

        DHParameterSpec dhParam = ((DHPublicKey)otherPub).getParams();

        KeyPairGenerator kPairGen = KeyPairGenerator.getInstance("DH");
        kPairGen.initialize(dhParam);
        KeyPair kPair = kPairGen.generateKeyPair();

        KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
        keyAgree.init(kPair.getPrivate());

        other.sharePub(kPair.getPublic().getEncoded()); //send encoded public key to other party

        keyAgree.doPhase(otherPub, true);
        byte[] sharedSecret = keyAgree.generateSecret();
        System.out.println(toHexString(sharedSecret));

    }

    public void sharePub(byte[] otherPubEnc) throws Exception{
        
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(otherPubEnc);
        PublicKey otherPub = keyFac.generatePublic(x509KeySpec);

        keyAgree.doPhase(otherPub, true);
        byte[] sharedSecret = keyAgree.generateSecret();
        System.out.println(toHexString(sharedSecret));

    }


    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
     * Converts a byte array to hex string
     */
    private static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len-1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }


    public void generateCipher(){

        cipher = null;

    }
        
    private PrivateKey getPrivate() throws Exception {
  
        byte[] keyBytes = Files.readAllBytes(Paths.get("keys/private-" + id + "/private.der"));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(spec);

    }

    private PublicKey getPublic(String messenger) throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get("keys/public/" + messenger + ".der"));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(spec);

    }

}