package se.nbis.lega.inbox.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class Credentials {

    @SerializedName("password_hash")
    private String passwordHash;

    @SerializedName("pubkey")
    private String publicKey;

    @SerializedName("expiration")
    private Float expiration;

}
