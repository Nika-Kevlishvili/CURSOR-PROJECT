package bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "power_supply_disconnection_request_pods", schema = "receivable")
public class DisconnectionPowerSupplyRequestsPods extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_disconnection_request_pods_id_seq",
            schema = "receivable",
            sequenceName = "power_supply_disconnection_request_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_request_pods_id_seq"
    )
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "product_contract_id")
    private Long productContractId;

    @Column(name = "alt_invoice_recipient_customer_id")
    private Long altInvoiceRecipientCustomerId;

    @Column(name = "is_checked")
    private Boolean isChecked;

    @Column(name = "power_supply_disconnection_request_id")
    private Long powerSupplyDisconnectionRequestId;

    @Column(name = "pod_with_highest_consumption")
    private Boolean podWithHighestConsumption;

}
