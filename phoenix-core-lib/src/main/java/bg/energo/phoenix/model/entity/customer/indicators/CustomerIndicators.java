package bg.energo.phoenix.model.entity.customer.indicators;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customer_indicators", schema = "reporting")
public class CustomerIndicators {

    @Id
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "number_of_active_contracts", nullable = false)
    private Long numberOfActiveContracts;

    @Column(name = "expiring_contracts_next_3_months", nullable = false)
    private Long expiringContractsNext3Months;

    @Column(name = "list_of_expiring_contracts_next_3_months")
    private String listOfExpiringContractsNext3Months;

    @Column(name = "payment_channel_name")
    private String paymentChannelName;

    @Column(name = "communication_channel")
    private String communicationChannel;

    @Column(name = "current_overdue_liabilities")
    private BigDecimal currentOverdueLiabilities;

    @Column(name = "current_receivables")
    private BigDecimal currentReceivables;

    @Column(name = "total_invoiced_amount")
    private BigDecimal totalInvoicedAmount;

    @Column(name = "average_payment_day")
    private BigDecimal averagePaymentDay;

    @Column(name = "active_rescheduling")
    private Integer activeRescheduling;

    @Column(name = "overdue_liabilities_12_month")
    private Integer overdueLiabilities12Month;

    @Column(name = "overdue_liabilities_24_month")
    private Integer overdueLiabilities24Month;

    @Column(name = "overdue_liabilities_12_month_with_data", nullable = false)
    private Integer overdueLiabilities12MonthWithData;

    @Column(name = "overdue_liabilities_24_month_with_data", nullable = false)
    private Integer overdueLiabilities24MonthWithData;

    @Column(name = "warnings_12_month", nullable = false)
    private Integer warnings12Month;

    @Column(name = "warnings_24_month", nullable = false)
    private Integer warnings24Month;

    @Column(name = "warnings_12_month_with_data", nullable = false)
    private Integer warnings12MonthWithData;

    @Column(name = "warnings_24_month_with_data", nullable = false)
    private Integer warnings24MonthWithData;

    @Column(name = "disconnection_12_month", nullable = false)
    private Integer disconnection12Month;

    @Column(name = "disconnection_24_month", nullable = false)
    private Integer disconnection24Month;

    @Column(name = "disconnection_12_month_with_data", nullable = false)
    private Integer disconnection12MonthWithData;

    @Column(name = "disconnection_24_month_with_data", nullable = false)
    private Integer disconnection24MonthWithData;

    @Column(name = "pav_last_12_months")
    private BigDecimal pavLast12Months;

    @Column(name = "pav_since_contract_start")
    private String pavSinceContractStart;

    @Column(name = "pav_agreed")
    private String pavAgreed;

    @Column(name = "deviation_last_12_months")
    private String deviationLast12Months;

    @Column(name = "deviation_since_contract_start")
    private String deviationSinceContractStart;

    @Column(name = "avg_daily_last_12_months")
    private BigDecimal avgDailyLast12Months;

    @Column(name = "avg_monthly_last_12_months")
    private BigDecimal avgMonthlyLast12Months;

    @Column(name = "max_monthly_last_12_months")
    private BigDecimal maxMonthlyLast12Months;

    @Column(name = "avg_daily_since_contract_start")
    private String avgDailySinceContractStart;

    @Column(name = "avg_monthly_since_contract_start")
    private String avgMonthlySinceContractStart;

    @Column(name = "max_monthly_since_contract_start")
    private String maxMonthlySinceContractStart;

    @Column(name = "avg_invoiced_last_12_months")
    private BigDecimal avgInvoicedLast12Months;

    @Column(name = "max_invoiced_last_12_months")
    private BigDecimal maxInvoicedLast12Months;

    @Column(name = "avg_invoiced_since_contract_start")
    private String avgInvoicedSinceContractStart;

    @Column(name = "max_invoiced_since_contract_start")
    private String maxInvoicedSinceContractStart;

    @Column(name = "current_liabilities")
    private BigDecimal currentLiabilities;

    @Transient
    private LocalDate assessmentDate;

    @Transient
    private String currentRiskAssessment;

    @Transient
    private Integer numberOfLawsuits;
}
