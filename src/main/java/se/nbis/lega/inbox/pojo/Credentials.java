package se.nbis.lega.inbox.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

/**
 * POJO for CEGA credentials.
 */
@ToString
@Data
public class Credentials {

    @JsonProperty("passwordHash")
    private String passwordHash;

    @JsonProperty("sshPublicKey")
    private String publicKey;

}
