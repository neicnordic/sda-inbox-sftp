package se.nbis.lega.inbox.sftp;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Crypt;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
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
import java.util.concurrent.TimeUnit;

/**
 * Component that authenticates users against the inbox.
 */
@Slf4j
@Component
public class InboxAuthenticator implements PublickeyAuthenticator, PasswordAuthenticator {

    private float defaultCacheTTL;
    private String inboxFolder;
    private CredentialsProvider credentialsProvider;
    private VirtualFileSystemFactory fileSystemFactory;

    // Caffeine cache with entry-specific TTLs
    private LoadingCache<String, Credentials> credentialsCache = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Credentials>() {
                public long expireAfterCreate(String key, Credentials graph, long currentTime) {
                    float ttl = graph.getExpiration() == null ? defaultCacheTTL : graph.getExpiration();
                    return TimeUnit.SECONDS.toNanos((long) ttl);
                }

                public long expireAfterUpdate(String key, Credentials graph, long currentTime, long currentDuration) {
                    return Long.MAX_VALUE;
                }

                public long expireAfterRead(String key, Credentials graph, long currentTime, long currentDuration) {
                    return Long.MAX_VALUE;
                }
            })
            .build(key -> credentialsProvider.getCredentials(key));

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate(String username, String password, ServerSession session) throws PasswordChangeRequiredException {
        try {
            Credentials credentials = credentialsCache.get(username);
            String hash = credentials.getPasswordHash();
            String[] hashParts = hash.split("\\$");
            String salt = String.format("$%s$%s$", hashParts[1], hashParts[2]);
            boolean result = ObjectUtils.nullSafeEquals(hash, Crypt.crypt(password, salt));
            if (result) {
                createHomeDir(inboxFolder, username);
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        try {
            Credentials credentials = credentialsCache.get(username);
            String publicKey = credentials.getPublicKey();
            RSAPublicKey rsaPublicKey = readKey(publicKey);
            boolean result = Arrays.equals(rsaPublicKey.getEncoded(), key.getEncoded());
            if (result) {
                createHomeDir(inboxFolder, username);
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private void createHomeDir(String inboxFolder, String username) {
        if (!inboxFolder.endsWith(File.separator)) {
            inboxFolder = inboxFolder + File.separator;
        }
        log.info(inboxFolder);
        File home = new File(inboxFolder + username);
        home.mkdirs();
        fileSystemFactory.setUserHomeDir(username, home.toPath());
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

    @Value("${inbox.cache.ttl}")
    public void setDefaultCacheTTL(float defaultCacheTTL) {
        this.defaultCacheTTL = defaultCacheTTL;
    }

    @Value("${inbox.directory}")
    public void setInboxFolder(String inboxFolder) {
        this.inboxFolder = inboxFolder;
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
