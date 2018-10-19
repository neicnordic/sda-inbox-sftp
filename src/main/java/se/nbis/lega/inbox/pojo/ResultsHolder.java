package se.nbis.lega.inbox.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

import java.util.Collection;

@ToString
@Data
public class ResultsHolder {

    @SerializedName("result")
    private Collection<Credentials> credentials;

}
