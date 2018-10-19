package se.nbis.lega.inbox.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.Collection;

@ToString
@Data
public class ResultsHolder {

    @JsonProperty("result")
    private Collection<Credentials> credentials;

}
