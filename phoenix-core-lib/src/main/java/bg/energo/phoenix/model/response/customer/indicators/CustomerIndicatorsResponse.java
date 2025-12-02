package bg.energo.phoenix.model.response.customer.indicators;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerIndicatorsResponse {

    private Long customerId;

    private Long numberOfActiveContracts;
    private Long expiringContractsNext3Months;
    private List<ExpiringContract> listOfExpiringContractsNext3Months;

    private String paymentChannelName;
    private String communicationChannel;

    private BigDecimal currentOverdueLiabilities;
    private BigDecimal currentReceivables;
    private BigDecimal totalInvoicedAmount;
    private BigDecimal averagePaymentDay;

    private Integer activeRescheduling;
    private Integer overdueLiabilities12Month;
    private Integer overdueLiabilities24Month;
    private Integer overdueLiabilities12MonthWithData;
    private Integer overdueLiabilities24MonthWithData;

    private Integer warnings12Month;
    private Integer warnings24Month;
    private Integer warnings12MonthWithData;
    private Integer warnings24MonthWithData;

    private Integer disconnection12Month;
    private Integer disconnection24Month;
    private Integer disconnection12MonthWithData;
    private Integer disconnection24MonthWithData;

    private BigDecimal pavLast12Months;
    private String pavSinceContractStart;
    private String pavAgreed;
    private String deviationLast12Months;
    private String deviationSinceContractStart;

    private BigDecimal avgDailyLast12Months;
    private BigDecimal avgMonthlyLast12Months;
    private BigDecimal maxMonthlyLast12Months;

    private String avgDailySinceContractStart;
    private String avgMonthlySinceContractStart;
    private String maxMonthlySinceContractStart;

    private BigDecimal avgInvoicedLast12Months;
    private BigDecimal maxInvoicedLast12Months;

    private String avgInvoicedSinceContractStart;
    private String maxInvoicedSinceContractStart;

    private BigDecimal currentLiabilities;

    private LocalDate assessmentDate;
    private String currentRiskAssessment;
    private Integer numberOfLawsuits;
}