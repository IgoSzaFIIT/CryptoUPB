package main;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Arrays;

public class CryptoUPB {

    public static byte[] encrypt(byte[] fileContent) throws Exception {
        System.out.println("IN ENCR");
        return doCrypto(Cipher.ENCRYPT_MODE, fileContent);
    }

    public static byte[] decrypt(byte[] inputFile, File privateKey) throws Exception {
       return doCrypto(Cipher.DECRYPT_MODE, inputFile);
    }

    private static byte[] doCrypto(int cipherMode, byte[] fileContent) throws CryptoException, Exception {
        try {
            if (cipherMode == 1) { // encrypt

                System.out.println("IN ENCRYPT");

                //vytvara sa kluc na symetricke sifrovanie
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);

                SecretKey key = keyGenerator.generateKey();
                //byte[] IV = new byte[16];
                //SecureRandom random = new SecureRandom();
                //random.nextBytes(IV);

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
                //IvParameterSpec ivSpec = new IvParameterSpec(IV);
                cipher.init(cipherMode, keySpec);


                byte[] outputBytes = cipher.doFinal(fileContent);
                return  outputBytes;
            }else{
                FileInputStream inputStream = new FileInputStream(inputFile);
                byte[] inputBytes = new byte[(int) inputFile.length()];
                inputStream.read(inputBytes);
                System.out.println("inputstream  " + 	inputBytes.length);
                FileInputStream key_fis = new FileInputStream(RSAKey);
                ObjectInputStream ois = new ObjectInputStream(key_fis);
                BigInteger modulus = (BigInteger) ois.readObject();
                BigInteger exponent = (BigInteger) ois.readObject();
                RSAPrivateKeySpec rsaPrivKSpec = new RSAPrivateKeySpec(modulus,exponent);

                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = kf.generatePrivate(rsaPrivKSpec);
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);

                byte[] ct_RSA = Arrays.copyOfRange(inputBytes, 0, 256);
                System.out.println("cyphertext " + new String(ct_RSA) + " dd " + ct_RSA.length );
                byte[] Key_IV = cipher.doFinal(ct_RSA);


                System.out.println("jkey " + new String(Key_IV) );
                byte[] key = Arrays.copyOfRange(Key_IV, 0, 16);
                byte[] IV = Arrays.copyOfRange(Key_IV, 16, 32);

                byte[] input = Arrays.copyOfRange(inputBytes, 256, inputBytes.length);
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

                // Create SecretKeySpec
                SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

                // Create IvParameterSpec
                IvParameterSpec ivSpec = new IvParameterSpec(IV);

                // Initialize Cipher for DECRYPT_MODE
                cipher.init(cipherMode, keySpec, ivSpec);

                byte[] outputBytes = cipher.doFinal(input);

                FileOutputStream outputStream = new FileOutputStream(outputFile);
                outputStream.write(outputBytes);
                inputStream.close();
                ois.close();
                outputStream.close();


            }
        }catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException | IOException ex) {
            throw new CryptoException("Error encrypting/decrypting file", ex);
        }
    }
}
