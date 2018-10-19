package se.nbis.lega.inbox.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

/**
 * POJO for CEGA credentials.
 */
@ToString
@Data
public class Credentials {

    @SerializedName("passwordHash")
    private String passwordHash;

    @SerializedName("sshPublicKey")
    private String publicKey;

}
