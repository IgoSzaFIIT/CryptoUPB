package main;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;


public class CryptoUPB {

    public static byte[] encrypt(byte[]inputFile, String publicKey) throws Exception {
        System.out.println("IN ENCRYPTION");
        return doCrypto(Cipher.ENCRYPT_MODE, inputFile, publicKey);
    }

    public static byte[] decrypt(byte[] inputFile, String privateKey) throws Exception {
       return doCrypto(Cipher.DECRYPT_MODE, inputFile, privateKey);
    }

    private static byte[] doCrypto(int cipherMode, byte[] fileContent, String RSAKey) throws CryptoException, Exception {
        try {
            if (cipherMode == Cipher.ENCRYPT_MODE) { // encrypt

                //Symetricky kluc vdaka asymetrickemu sifrovaniu nemusime ukladat na servery, tak ho vygenerujeme tu
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);
                SecretKey key = keyGenerator.generateKey();
                byte[] IV = new byte[16];
                SecureRandom random = new SecureRandom();
                random.nextBytes(IV);
                IvParameterSpec ivSpec = new IvParameterSpec(IV);



                //symetricke sifrovanie cez AES
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
                cipher.init(cipherMode, keySpec, ivSpec);


                // dostaneme zasifrovane byte, ktore posielame do FileHandler, kde sa z nich spravi File
                byte[] outputBytes = cipher.doFinal(fileContent);
               

                byte[] mac = generateMACFromMessage(key, outputBytes);

                //System.out.println("MAC: " + mac);
                //System.out.println("dlzka MAC : "  + mac.length);


                byte[] publicBytes = Base64.getDecoder().decode(RSAKey);//.decodeBase64(publicK);
                X509EncodedKeySpec KeySpec = new X509EncodedKeySpec(publicBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(KeySpec);

//                //priprava sifrovania symetrickeho kluca RSA algoritmom
                cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(cipherMode, publicKey);
                //

                // tu sa asi robi miesto do hlavicky na tie metadata atd
                byte[] plainTextRSA = new byte[IV.length + key.getEncoded().length];
                System.arraycopy(key.getEncoded(), 0, plainTextRSA, 0, key.getEncoded().length);
                System.arraycopy(IV, 0, plainTextRSA, key.getEncoded().length, IV.length);
//                byte[] plainTextRSA = new byte[key.getEncoded().length];
//                System.arraycopy(key.getEncoded(), 0, plainTextRSA, 0, key.getEncoded().length);
//

                //System.out.println(plainTextRSA);


                //zasifrovanie symetrickej sifry RSAckom
                byte[] RSA_output = cipher.doFinal(plainTextRSA);

                //System.out.println("bytes = " + RSA_output.length);
                //System.out.println("key_bytes = " + key.getEncoded().length);
                //System.out.println("iv_bytes = " + IV.length);

                //vysledok kde je spojena hlavicka, kde je zasifrovany kluc a zvysok je zasifrovany text
                byte[] AES_output = new byte[mac.length + RSA_output.length + outputBytes.length];

                System.arraycopy(mac, 0, AES_output, 0, mac.length);
                System.arraycopy(RSA_output, 0, AES_output, mac.length, RSA_output.length);
                System.arraycopy(outputBytes, 0, AES_output, mac.length + RSA_output.length, outputBytes.length);

                return  AES_output;

            }else{
                //nacitanie RSAKey a rozparsovanie
//                FileInputStream key_fis = new FileInputStream(RSAKey);
//                ObjectInputStream ois = new ObjectInputStream(key_fis);
//                BigInteger modulus = (BigInteger) ois.readObject();
//                BigInteger exponent = (BigInteger) ois.readObject();
//                RSAPrivateKeySpec rsaPrivKSpec = new RSAPrivateKeySpec(modulus,exponent);



                byte[] publicBytes = Base64.getDecoder().decode(RSAKey);//.decodeBase64(publicK);

//                if(publicBytes != null){
//                    return publicBytes;
//                }

                PKCS8EncodedKeySpec KeySpec = new PKCS8EncodedKeySpec(publicBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = keyFactory.generatePrivate(KeySpec);
                //

//                //vytvorenie privateKey
//                KeyFactory kf = KeyFactory.getInstance("RSA");
//                PrivateKey privateKey = kf.generatePrivate(rsaPrivKSpec);
//                //



                //priprava na desifrovanie RSA sifry
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);

                byte[] ct_RSA = Arrays.copyOfRange(fileContent, 0+32, 256+32);
                //System.out.println("cyphertext " + new String(ct_RSA) + " dd " + ct_RSA.length );
                byte[] Key_IV = cipher.doFinal(ct_RSA);


                //System.out.println("jkey " + new String(Key_IV) );

                //mac pripraveny na kontrolu
                byte[] mac = Arrays.copyOfRange(fileContent, 0, 32);


                byte[] key = Arrays.copyOfRange(Key_IV, 0, 16);
                byte[] IV = Arrays.copyOfRange(Key_IV, 16, 32);

                fileContent = Arrays.copyOfRange(fileContent, 256+32, fileContent.length);
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

                // Create SecretKeySpec
                SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

                // Create IvParameterSpec
                IvParameterSpec ivSpec = new IvParameterSpec(IV);

                // Initialize Cipher for DECRYPT_MODE
                cipher.init(cipherMode, keySpec, ivSpec);
                //System.out.println(fileContent);

                /* To handle larger files */
                ByteArrayInputStream in = new ByteArrayInputStream(fileContent);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                CipherOutputStream cos = new CipherOutputStream(outputStream, cipher);
                byte[] buf = new byte[1024];
                int read;
                while((read=in.read(buf))!=-1){
                    cos.write(buf,0,read);
                }
                in.close();
                outputStream.flush();
                cos.close();

                return outputStream.toByteArray();
                
                /*
                byte[] outputBytes = cipher.doFinal(fileContent);


                return outputBytes;*/
            }
        }catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException ex) {
            throw new CryptoException("Error encrypting/decrypting file", ex);
        }
    }

    public static byte[] generateMACFromMessage (SecretKey key, byte[] msg) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] macResult = mac.doFinal(msg);
        return macResult;
    }



}
