package se.nbis.lega.inbox.sftp;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.io.IOUtils;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import se.nbis.lega.inbox.pojo.Credentials;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.concurrent.TimeUnit;

import static org.apache.sshd.common.config.keys.KeyUtils.DSS_ALGORITHM;
import static org.apache.sshd.common.config.keys.KeyUtils.RSA_ALGORITHM;

/**
 * Component that authenticates users against the inbox.
 */
@Slf4j
@Component
public class InboxAuthenticator implements PublickeyAuthenticator, PasswordAuthenticator {

    private long defaultCacheTTL;
    private String inboxFolder;
    private CredentialsProvider credentialsProvider;
    private VirtualFileSystemFactory fileSystemFactory;

    // Caffeine cache with entry-specific TTLs
    private LoadingCache<String, Credentials> credentialsCache = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Credentials>() {
                public long expireAfterCreate(String key, Credentials graph, long currentTime) {
                    return TimeUnit.SECONDS.toNanos(defaultCacheTTL);
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
            boolean result = StringUtils.startsWithIgnoreCase(hash, "$2")
                    ? BCrypt.checkpw(password, hash)
                    : ObjectUtils.nullSafeEquals(hash, Crypt.crypt(password, hash));

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
            PublicKey publicKey = readKey(credentials.getPublicKey());
            boolean result = KeyUtils.compareKeys(publicKey, key);
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
        log.info("Inbox initialized: {}", inboxFolder + username);
        File home = new File(inboxFolder + username);
        home.mkdirs();
        fileSystemFactory.setUserHomeDir(username, home.toPath());
    }

    // according to https://tools.ietf.org/html/rfc4253#section-6.6
    private PublicKey readKey(String key) throws IOException, GeneralSecurityException {
        String keyFormat = key.split(" ")[0];
        if (!"ssh-rsa".equals(keyFormat) && !"ssh-dss".equals(keyFormat)) {
            throw new InvalidKeySpecException(String.format("Unsupported key format: %s", keyFormat));
        }
        byte[] keyBytes = Base64.decodeBase64(key.split(" ")[1]);
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(keyBytes));
        byte[] header = readElement(inputStream);
        String encodedKeyFormat = new String(header);
        if (!keyFormat.equals(encodedKeyFormat)) {
            throw new InvalidKeySpecException(String.format("Unsupported key format: %s", encodedKeyFormat));
        }
        if ("ssh-rsa".equals(encodedKeyFormat)) {
            BigInteger publicExponent = new BigInteger(readElement(inputStream));
            BigInteger modulus = new BigInteger(readElement(inputStream));
            KeyFactory keyFactory = SecurityUtils.getKeyFactory(RSA_ALGORITHM);
            return keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
        }
        if ("ssh-dss".equals(encodedKeyFormat)) {
            BigInteger p = new BigInteger(readElement(inputStream));
            BigInteger q = new BigInteger(readElement(inputStream));
            BigInteger g = new BigInteger(readElement(inputStream));
            BigInteger y = new BigInteger(readElement(inputStream));
            KeyFactory keyFactory = SecurityUtils.getKeyFactory(DSS_ALGORITHM);
            return keyFactory.generatePublic(new DSAPublicKeySpec(y, p, q, g));
        }
        throw new InvalidKeySpecException(String.format("Unsupported key format: %s", encodedKeyFormat));
    }

    private byte[] readElement(DataInputStream dataInputStream) throws IOException {
        int blockLength = dataInputStream.readInt();
        return IOUtils.readFully(dataInputStream, blockLength);
    }

    @Value("${inbox.cache.ttl}")
    public void setDefaultCacheTTL(long defaultCacheTTL) {
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
