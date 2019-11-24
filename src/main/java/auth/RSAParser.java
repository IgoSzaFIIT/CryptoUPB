package auth;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class RSAParser {

    // TODO : vseobecny concern : tato forma ktoru parsuje sa nemusi vzdy zhodovat, niektore kluce (zalezi od generatora) mozu mat nejake info navyse. Tym padom ostane aj po parsovani v kluci bordel a nemusia
    // TODO : ho metody v CryptoUPB prelozit. Teda pri parsovani a ukladani do DB, by bolo dobre vytvorit overenie, ci kluce funguje. Ked nie, vyhodit chybu, ze treba RSA generovane Java classmi PKCS8EncodedKeySpec, X509EncodedKeySpec

    //  volanie funkcii :
    //  pre rozparsovanie privateKey : getPrivateKey("fileName");
    //  pre rozparsovanie publicKey : getPublicKey("fileName");

    //Fukcia pre natlacenie File do String
    private String getKey(String path) throws IOException {
        String strKeyPEM = "";
        System.out.println("CESTA ....." + path);
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        while ((line = br.readLine()) != null) {
            strKeyPEM += line + "\n";
        }
        br.close();
        return strKeyPEM;
    }

    //Funkcia vracajuca private key
    public String getPrivateKey(String path) throws IOException, GeneralSecurityException {
        String privateKeyPEM = getKey(path);
        return getPrivateKeyFromString(privateKeyPEM);
    }

    //Zbavi povodny file zbytocnych prvkov, vrati iba kluc (forma zbytocnosti, resp zbytocny text sa moze menit, treba debug a skontrolovat co vsetko vyhodit)
    public String getPrivateKeyFromString(String key) throws IOException, GeneralSecurityException {
        String privateKeyPEM = key;
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
        privateKeyPEM = privateKeyPEM.replace("\n", "");
        return privateKeyPEM;
    }

    //Funkcia vracajuca public key
    public String getPublicKey(String path) throws IOException, GeneralSecurityException {
        String publicKeyPEM = getKey(path);
        return getPublicKeyFromString(publicKeyPEM);
    }

    //Zbavi povodny file zbytocnych prvkov, vrati iba kluc (forma zbytocnosti, resp zbytocny text sa moze menit, treba debug a skontrolovat co vsetko vyhodit)
    public String getPublicKeyFromString(String key) throws IOException, GeneralSecurityException {
        String publicKeyPEM = key;
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
        publicKeyPEM = publicKeyPEM.replace("\n", "");
        return publicKeyPEM;
    }

    //Funkcia vracajuca obaleny private key
    public String setPrivateKey(String privateKey) throws IOException, GeneralSecurityException {
        return setPrivateKeyFromString(privateKey);
    }

    //Prida do vygenerovaneho stringu zbytocnosti, aby sa formou zhodoval s parsovanymi
    public String setPrivateKeyFromString(String key) throws IOException, GeneralSecurityException {
        String privateKey_key = key;
        String privateKey_beginning = "-----BEGIN PRIVATE KEY-----\n";
        String privateKey_end = "\n-----END PRIVATE KEY-----";
        String privateKey = privateKey_beginning + privateKey_key + privateKey_end;
        return privateKey;
    }

    //Funkcia vracajuca obaleny public key
    public String setPublicKey(String publicKey) throws IOException, GeneralSecurityException {
        return setPublicKeyFromString(publicKey);
    }

    //Prida do vygenerovaneho stringu zbytocnosti, aby sa formou zhodoval s parsovanymi
    public String setPublicKeyFromString(String key) throws IOException, GeneralSecurityException {
        String publicKey_key = key;
        String publicKey_beginning = "-----BEGIN PUBLIC KEY-----\n";
        String publicKey_end = "\n-----END PUBLIC KEY-----";
        String publicKey = publicKey_beginning + publicKey_key + publicKey_end;
        return publicKey;
    }
}
