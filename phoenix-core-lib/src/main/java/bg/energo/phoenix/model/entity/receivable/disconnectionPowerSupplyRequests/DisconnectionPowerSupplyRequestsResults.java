package bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "power_supply_disconnection_request_results", schema = "receivable")
public class DisconnectionPowerSupplyRequestsResults extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_disconnection_request_results_id_seq",
            schema = "receivable",
            sequenceName = "power_supply_disconnection_request_results_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_request_results_id_seq"
    )
    private Long id;

    @Column(name = "customers")
    private String customers;

    @Column(name = "contracts")
    private String contracts;

    @Column(name = "alt_recipient_inv_customers")
    private String altRecipientInvCustomers;

    @Column(name = "billing_groups")
    private String billingGroups;

    @Column(name = "is_highest_consumption")
    private Boolean isHighestConsumption;

    @Column(name = "liabilities_in_billing_group")
    private String liabilitiesInBillingGroup;

    @Column(name = "liabilities_in_pod")
    private String liabilitiesInPod;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "power_supply_disconnection_request_id")
    private Long powerSupplyDisconnectionRequestId;

    @Column(name = "existing_customer_receivables")
    private Boolean existingCustomerReceivables;

    @Column(name = "is_checked")
    private Boolean isChecked;

    @Column(name = "pod_identifier")
    private String podIdentifier;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "liability_amount_customer")
    private BigDecimal liabilityAmountCustomer;

    @Column(name = "customer_number")
    private String customerNumber;

    @Column(name = "pod_detail_id")
    private Long podDetailId;

    @Column(name = "saved_invoice_id")
    private Long savedInvoiceId;

}
