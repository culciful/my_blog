package com.culciful.security.crypto;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

/**
 * 前端用公钥加密、后端用私钥解密（{@link #decryptWithPrivate}）。
 * 密钥对在应用启动时生成一次；私钥不对外暴露。
 */
public class RSAUtil {

    private static final int KEY_SIZE = 1024;

    private static KeyPair keyPair;
    private static String publicKeyBase64;

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
            generator.initialize(KEY_SIZE, new SecureRandom());
            keyPair = generator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            publicKeyBase64 = new String(Base64.encodeBase64(publicKey.getEncoded(), false));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key pair init failed", e);
        }
    }

    public static String decryptWithPrivate(String encryptText) throws Exception {
        if (StringUtils.isBlank(encryptText)) {
            return null;
        }
        byte[] enByte = Base64.decodeBase64(encryptText);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", getBouncyCastleProvider());
        PrivateKey privateKey = keyPair.getPrivate();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] res = cipher.doFinal(enByte);
        return new String(res);
    }

    public static String getPublicKey() {
        return publicKeyBase64;
    }
}
