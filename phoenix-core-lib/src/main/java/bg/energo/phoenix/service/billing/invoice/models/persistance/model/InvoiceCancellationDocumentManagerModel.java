package bg.energo.phoenix.service.billing.invoice.models.persistance.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InvoiceCancellationDocumentManagerModel {
    @JsonProperty("title")
    public String Title;
    @JsonProperty("name")
    public String Name;
    @JsonProperty("surname")
    public String Surname;
    @JsonProperty("jobPosition")
    public String JobPosition;
}
