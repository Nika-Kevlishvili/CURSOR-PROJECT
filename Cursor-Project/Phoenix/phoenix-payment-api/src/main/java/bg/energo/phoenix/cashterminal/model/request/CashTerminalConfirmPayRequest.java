package bg.energo.phoenix.cashterminal.model.request;

import bg.energo.phoenix.cashterminal.model.enums.CashTerminalRequestType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashTerminalConfirmPayRequest {

    @NotNull(message = "Date must not be null")
    @JsonProperty("DATE")
    @Schema(name = "DATE")
    private String DATE;

    @NotNull(message = "Type must not be null")
    @JsonProperty("TYPE")
    @Schema(name = "TYPE")
    private CashTerminalRequestType TYPE;

    @NotNull(message = "Merchant id must not be null")
    @JsonProperty("MERCHANTID")
    @Schema(name = "MERCHANTID")
    private String MERCHANTID;

    @NotNull(message = "Check idn must not be null")
    @JsonProperty("IDN")
    @Schema(name = "IDN")
    private String IDN;

    @NotNull(message = "Total amount must not be null")
    @JsonProperty("TOTAL")
    @Schema(name = "TOTAL")
    private Long TOTAL;

    @NotNull(message = "Transaction id must not be null")
    @JsonProperty("TID")
    @Schema(name = "TID")
    private String TID;

    @JsonProperty("INVOICES")
    @Schema(name = "INVOICES")
    private String INVOICES;

    @JsonIgnore
    public List<String> getInvoiceNumbers() {
        if (StringUtils.isNotEmpty(INVOICES)) {
            return List.of(INVOICES.split(","));
        } else {
            return new ArrayList<>();
        }
    }

    @JsonIgnore
    public List<Long> getLiabilityIds() {
        if (StringUtils.isNotEmpty(INVOICES)) {
            return Arrays
                    .stream(INVOICES.split(","))
                    .map(invoice -> Long.parseLong(invoice.split("\\.")[1]))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}

