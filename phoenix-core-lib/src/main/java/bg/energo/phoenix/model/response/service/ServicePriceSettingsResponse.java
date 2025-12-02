package bg.energo.phoenix.model.response.service;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServicePriceSettingsResponse {

    private Boolean equalMonthlyInstallmentsActivation;

    private Short installmentNumber;

    private Short installmentNumberFrom;

    private Short installmentNumberTo;

    private BigDecimal amount;

    private BigDecimal amountFrom;

    private BigDecimal amountTo;

    private Long currencyId;

    private String currencyName;

}
