package se.nbis.lega.inbox.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class ResponseHolder {

    @SerializedName("response")
    private ResultsHolder resultsHolder;

}
