package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "manual_debit_or_credit_note_invoice_summary_data", schema = "invoice")
public class ManualDebitOrCreditNoteInvoiceSummaryData extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "manual_debit_or_credit_note_invoice_summary_data_id_gen")
    @SequenceGenerator(name = "manual_debit_or_credit_note_invoice_summary_data_id_gen", schema = "invoice", sequenceName = "manual_debit_or_credit_note_invoice_summery_data_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 1024)
    @Column(name = "price_component_or_price_component_groups", length = 1024)
    private String priceComponentOrPriceComponentGroups;

    @Column(name = "total_volumes")
    private BigDecimal totalVolumes;

    @Size(max = 512)
    @Column(name = "measures_unit_for_total_volumes", length = 512)
    private String measuresUnitForTotalVolumes;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Size(max = 512)
    @Column(name = "measure_for_unit_price", length = 512)
    private String measureForUnitPrice;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "value_currency_id")
    private Long valueCurrencyId;

    @Size(max = 32)
    @Column(name = "income_account_number", length = 32)
    private String incomeAccountNumber;

    @Size(max = 32)
    @Column(name = "cost_center", length = 32)
    private String costCenter;

    @Column(name = "vat_rate_id")
    private Long vatRateId;

    @NotNull
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "vat_rate_percent")
    private BigDecimal vatRatePercent;

    @Column(name = "value_currency_exchange_rate")
    private BigDecimal valueCurrencyExchangeRate;

    @Size(max = 1024)
    @Column(name = "value_currency_name", length = 1024)
    private String valueCurrencyName;

    @Size(max = 1024)
    @Column(name = "vat_rate_name", length = 1024)
    private String vatRateName;


    public ManualDebitOrCreditNoteInvoiceSummaryData cloneForReversal(Long invoiceId) {
        return new ManualDebitOrCreditNoteInvoiceSummaryData(
                null,
                this.priceComponentOrPriceComponentGroups,
                this.totalVolumes,
                this.measuresUnitForTotalVolumes,
                this.unitPrice,
                this.measureForUnitPrice,
                this.value,
                this.valueCurrencyId,
                this.incomeAccountNumber,
                this.costCenter,
                this.vatRateId,
                invoiceId,
                this.vatRatePercent,
                this.valueCurrencyExchangeRate,
                this.valueCurrencyName,
                this.vatRateName
        );
    }

}