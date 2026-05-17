package com.culciful.security.crypto;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;

import javax.crypto.Cipher;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Use decryptWithPrivate to decrypt data encrypted by the frontend with the public key.
 * A new RSA key pair is generated after each application restart.
 */
public class RSAUtil {

    private static final int KEY_SIZE = 1024;

    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY = "publicKey";

    private static KeyPair keyPair;
    private static Map<String, String> rsaMap;

    private static org.bouncycastle.jce.provider.BouncyCastleProvider bouncyCastleProvider = null;

    public static synchronized org.bouncycastle.jce.provider.BouncyCastleProvider getBouncyCastleProvider() {
        if (bouncyCastleProvider == null) {
            bouncyCastleProvider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        }
        return bouncyCastleProvider;
    }

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", getBouncyCastleProvider());
            SecureRandom random = new SecureRandom();
            generator.initialize(KEY_SIZE, random);
            keyPair = generator.generateKeyPair();
            storeRSA();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void storeRSA() {
        rsaMap = new HashMap<>();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyStr = new String(Base64.encodeBase64(publicKey.getEncoded(), false));
        rsaMap.put(PUBLIC_KEY, publicKeyStr);

        PrivateKey privateKey = keyPair.getPrivate();
        String privateKeyStr = new String(Base64.encodeBase64(privateKey.getEncoded(), false));
        rsaMap.put(PRIVATE_KEY, privateKeyStr);
    }

    public static String decryptWithPrivate(String encryptText) throws Exception {
        if (StringUtils.isBlank(encryptText)) {
            return null;
        }
        byte[] en_byte = Base64.decodeBase64(encryptText);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", getBouncyCastleProvider());
        PrivateKey privateKey = keyPair.getPrivate();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] res = cipher.doFinal(en_byte);
        return new String(res);
    }

    public static byte[] encrypt(String plaintext) throws UnsupportedEncodingException {
        String encode = URLEncoder.encode(plaintext, "utf-8");
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        BigInteger e = rsaPublicKey.getPublicExponent();
        BigInteger n = rsaPublicKey.getModulus();
        BigInteger m = new BigInteger(encode.getBytes());
        BigInteger res = m.modPow(e, n);
        return res.toByteArray();
    }

    public static String decrypt(byte[] cipherText) throws UnsupportedEncodingException {
        RSAPrivateKey prk = (RSAPrivateKey) keyPair.getPrivate();
        BigInteger d = prk.getPrivateExponent();
        BigInteger n = prk.getModulus();
        BigInteger c = new BigInteger(cipherText);
        BigInteger m = c.modPow(d, n);
        byte[] mt = m.toByteArray();
        String en = new String(mt);
        return URLDecoder.decode(en, "UTF-8");
    }

    public static String getPublicKey() {
        return rsaMap.get(PUBLIC_KEY);
    }

    public static String getPrivateKey() {
        return rsaMap.get(PRIVATE_KEY);
    }
}
