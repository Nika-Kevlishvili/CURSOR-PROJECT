package bg.energo.phoenix.apis.model;

import bg.energo.phoenix.model.response.nomenclature.customer.LegalFormResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h1>Apis customer</h1>
 * {@link #eik} personal number form apis
 * {@link #name} name number form apis
 * {@link #type} type number form apis
 * {@link #legalForms} Legal form nomenclature object
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApisCustomer {
    @JsonProperty("eik")
    private String eik;
    @JsonProperty("name")
    private String name;
    @JsonProperty("type")
    private String type;
    @JsonProperty("legalForms")
    private LegalFormResponse legalForms;
}
