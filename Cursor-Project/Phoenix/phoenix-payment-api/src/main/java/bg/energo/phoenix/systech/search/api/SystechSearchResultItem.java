package bg.energo.phoenix.systech.search.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystechSearchResultItem {

    @JsonProperty("CustomerNumber")
    private String CustomerNumber;

    @JsonProperty("CustomerName")
    private String CustomerName;

    @JsonProperty("CustomerAddress")
    private String CustomerAddress;

    @JsonProperty("CustomerPIN")
    private String CustomerPIN;

    @JsonProperty("CustomerPhone")
    private String CustomerPhone;
}
