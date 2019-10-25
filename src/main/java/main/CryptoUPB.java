package main;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

public class CryptoUPB {

    public static byte[] encrypt(byte[]inputFile, File publicKey) throws Exception {
        System.out.println("IN ENCR");
        return doCrypto(Cipher.ENCRYPT_MODE, inputFile, publicKey);
    }

    public static byte[] decrypt(byte[] inputFile, File privateKey) throws Exception {
       return doCrypto(Cipher.DECRYPT_MODE, inputFile, privateKey);
    }

    private static byte[] doCrypto(int cipherMode, byte[] fileContent, File RSAKey) throws CryptoException, Exception {
        try {
            if (cipherMode == 1) { // encrypt

                //Symetricky kluc vdaka asymetrickemu sifrovaniu nemusime ukladat na servery, tak ho vygenerujeme tu
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);
                SecretKey key = keyGenerator.generateKey();
                //
                //toto neviem k comu je skuste to zistit a hodit tu koment
                byte[] IV = new byte[16];
                SecureRandom random = new SecureRandom();
                random.nextBytes(IV);

                //symetricke sifrovanie cez AES
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
                cipher.init(cipherMode, keySpec);

                //neviem na co to je
                IvParameterSpec ivSpec = new IvParameterSpec(IV);

                // dostaneme zasifrovane byte, ktore posielame do FileHandler, kde sa z nich spravi File
                byte[] outputBytes = cipher.doFinal(fileContent);

                //kopia RSAKey
                FileInputStream key_fis = new FileInputStream(RSAKey);

                //deserializuje primitivne data
                ObjectInputStream ois = new ObjectInputStream(key_fis);
                BigInteger modulus = (BigInteger) ois.readObject();
                BigInteger exponent = (BigInteger) ois.readObject();

                //vytvorenie RSA public kluca
                RSAPublicKeySpec rsaPKSpec = new RSAPublicKeySpec(modulus,exponent);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PublicKey publicKey = kf.generatePublic(rsaPKSpec);
                //

                //priprava sifrovania symetrickeho kluca RSA algoritmom
                cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(cipherMode, publicKey);
                //

                // tu sa asi robi miesto do hlavicky na tie metadata atd
                byte[] plainTextRSA = new byte[IV.length + key.getEncoded().length];
                System.arraycopy(key.getEncoded(), 0, plainTextRSA, 0, key.getEncoded().length);
                System.arraycopy(IV, 0, plainTextRSA, key.getEncoded().length, IV.length);

                //zasifrovanie symetrickej sifry RSAckom
                byte[] RSA_output = cipher.doFinal(plainTextRSA);
                System.out.println("bytes = " + RSA_output.length);
                System.out.println("key_bytes = " + key.getEncoded().length);
                System.out.println("iv_bytes = " + IV.length);

                //vysledok kde je spojena hlavicka, kde je zasifrovany kluc a zvysok je zasifrovany text
                byte[] AES_output = new byte[RSA_output.length + outputBytes.length];

                System.arraycopy(RSA_output, 0, AES_output, 0, RSA_output.length);
                System.arraycopy(outputBytes, 0, AES_output, RSA_output.length, outputBytes.length);

                return  AES_output;

            }else{
                //nacitanie RSAKey a rozparsovanie
                FileInputStream key_fis = new FileInputStream(RSAKey);
                ObjectInputStream ois = new ObjectInputStream(key_fis);
                BigInteger modulus = (BigInteger) ois.readObject();
                BigInteger exponent = (BigInteger) ois.readObject();
                RSAPrivateKeySpec rsaPrivKSpec = new RSAPrivateKeySpec(modulus,exponent);
                //

                //vytvorenie privateKey
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = kf.generatePrivate(rsaPrivKSpec);
                //

                //priprava na desifrovanie RSA sifry
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);

                byte[] ct_RSA = Arrays.copyOfRange(fileContent, 0, 256);
                System.out.println("cyphertext " + new String(ct_RSA) + " dd " + ct_RSA.length );
                byte[] Key_IV = cipher.doFinal(ct_RSA);


                System.out.println("jkey " + new String(Key_IV) );
                byte[] key = Arrays.copyOfRange(Key_IV, 0, 16);
                byte[] IV = Arrays.copyOfRange(Key_IV, 16, 32);

                fileContent = Arrays.copyOfRange(fileContent, 256, fileContent.length);
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

                // Create SecretKeySpec
                SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

                // Create IvParameterSpec
                IvParameterSpec ivSpec = new IvParameterSpec(IV);

                // Initialize Cipher for DECRYPT_MODE
                cipher.init(cipherMode, keySpec, ivSpec);

                byte[] outputBytes = cipher.doFinal(fileContent);


                return outputBytes;
            }
        }catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException | IOException ex) {
            throw new CryptoException("Error encrypting/decrypting file", ex);
        }
    }
}
