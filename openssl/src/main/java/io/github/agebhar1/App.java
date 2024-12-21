package io.github.agebhar1;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class App {

    static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    static String encodeHexString(byte[] byteArray) {
        var builder = new StringBuilder();
        for (byte b : byteArray) {
            builder.append(byteToHex(b));
        }
        return builder.toString();
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    // https://medium.com/@patc888/decrypt-openssl-encrypted-data-in-java-4c31983afe19

    // OpenSSL 1.0.2
    // openssl enc [-debug] -aes-256-cbc -d -md sha256 -in src/main/java/io/github/agebhar1/App.java.enc
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        var password = new byte[]{'q'};

        var path = Paths.get(URI.create("file:///home/agebhar1/src/github.com/agebhar1/snippets/openssl/src/main/java/io/github/agebhar1/App.java.enc"));
        var cipherBytes = Files.readAllBytes(path);
        var salt = Arrays.copyOfRange(cipherBytes, 8, 16);
        System.out.println("salt: " + encodeHexString(salt));

        cipherBytes = Arrays.copyOfRange(cipherBytes, 16, cipherBytes.length);

        var passAndSalt = concat(password, salt);

        var md = MessageDigest.getInstance("sha256"); // md5
        var key1 = md.digest(passAndSalt);

        md.reset();
        md.update(key1);
        md.update(password);
        md.update(salt);

        var key2 = md.digest();
        var key = concat(key1, key2);
        System.out.println("key: " + encodeHexString(Arrays.copyOfRange(key, 0, 32)));

        var secretKey = new SecretKeySpec(Arrays.copyOfRange(key, 0, 32), "AES");
        var iv = Arrays.copyOfRange(key, 32, 32 + 16);
        System.out.println("iv: " + encodeHexString(iv));

        var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

        var clearText = new String(cipher.doFinal(cipherBytes));
        System.out.println(clearText);
    }
}
