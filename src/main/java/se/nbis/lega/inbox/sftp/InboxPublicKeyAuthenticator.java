package se.nbis.lega.inbox.sftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.nbis.lega.inbox.pojo.Credentials;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

@Slf4j
@Component
public class InboxPublicKeyAuthenticator implements PublickeyAuthenticator {

    private CredentialsProvider credentialsProvider;
    private VirtualFileSystemFactory fileSystemFactory;

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        try {
            Credentials credentials = credentialsProvider.getCredentials(username);
            String publicKey = credentials.getPublicKey();
            RSAPublicKey rsaPublicKey = readKey(publicKey);
            boolean result = Arrays.equals(rsaPublicKey.getEncoded(), key.getEncoded());
            if (result) {
                File home = new File(String.format("/Users/dmytrot/mina/%s", username));
                home.mkdirs();
                fileSystemFactory.setUserHomeDir(username, home.toPath());
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    // copied from https://github.com/CloudStack-extras/CloudStack-archive/blob/5b8d72bea4753fd9ecb500dd8db47b430cb7513a/utils/src/com/cloud/utils/crypt/RSAHelper.java
    private RSAPublicKey readKey(String key) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] encKey = Base64.decodeBase64(key.split(" ")[1]);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encKey));

        byte[] header = readElement(dis);
        String pubKeyFormat = new String(header);
        if (!pubKeyFormat.equals("ssh-rsa")) {
            throw new RuntimeException("Unsupported format");
        }

        byte[] publicExponent = readElement(dis);
        byte[] modulus = readElement(dis);

        KeySpec spec = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(publicExponent));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    // copied from https://github.com/CloudStack-extras/CloudStack-archive/blob/5b8d72bea4753fd9ecb500dd8db47b430cb7513a/utils/src/com/cloud/utils/crypt/RSAHelper.java
    private byte[] readElement(DataInput dis) throws IOException {
        int len = dis.readInt();
        byte[] buf = new byte[len];
        dis.readFully(buf);
        return buf;
    }

    @Autowired
    public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Autowired
    public void setFileSystemFactory(VirtualFileSystemFactory fileSystemFactory) {
        this.fileSystemFactory = fileSystemFactory;
    }

}
