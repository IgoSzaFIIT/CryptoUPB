package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.*;
import java.util.Base64;

public class KPGenerator {
    private KeyPair kp;
    private PublicKey pubKey;
    private PrivateKey privKey;
    private String encodedPubKey;
    private String encodedPrivKey;

    public KPGenerator() throws NoSuchAlgorithmException {
        setKp(generateKeypair());

        setPrivKey(this.kp.getPrivate());
        setPubKey(this.kp.getPublic());

        Base64.Encoder encoder = Base64.getEncoder();
        setEncodedPrivKey(encoder.encodeToString(this.privKey.getEncoded()));
        setEncodedPubKey(encoder.encodeToString(this.pubKey.getEncoded()));
    }

    public void saveKeys(String nameOfPrivateKeyFile, String nameOfPublicKeyFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(nameOfPrivateKeyFile));
        writer.write(this.encodedPrivKey);
        writer.close();

        BufferedWriter writer1 = new BufferedWriter(new FileWriter(nameOfPublicKeyFile));
        writer1.write(this.encodedPubKey);
        writer1.close();
    }

    private  KeyPair generateKeypair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        return kp;
    }

    public KeyPair getKp() {
        return kp;
    }

    public PublicKey getPubKey() {
        return pubKey;
    }

    public PrivateKey getPrivKey() {
        return privKey;
    }

    public String getEncodedPubKey() {
        return encodedPubKey;
    }

    public String getEncodedPrivKey() {
        return encodedPrivKey;
    }

    private void setKp(KeyPair kp) {
        this.kp = kp;
    }



    private void setPubKey(PublicKey pubKey) {
        this.pubKey = pubKey;
    }



    private void setPrivKey(PrivateKey privKey) {
        this.privKey = privKey;
    }



    private void setEncodedPubKey(String encodedPubKey) {
        this.encodedPubKey = encodedPubKey;
    }



    private void setEncodedPrivKey(String encodedPrivKey) {
        this.encodedPrivKey = encodedPrivKey;
    }




}
