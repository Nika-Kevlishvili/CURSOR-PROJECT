package bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingCreateRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "manual_liabilitie_offsettings", schema = "receivable")
public class ManualLiabilityOffsetting extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "manual_liabilitie_offsettings_id_seq",
            schema = "receivable",
            sequenceName = "manual_liabilitie_offsettings_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "manual_liabilitie_offsettings_id_seq"
    )
    private Long id;

    @Column(name = "manual_liabilitie_date")
    private LocalDate manualLiabilityDate;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "customer_communication_id_for_billing")
    private Long customerCommunicationId;

    @Column(name = "reversed")
    private boolean reversed;

    public ManualLiabilityOffsetting(ManualLiabilityOffsettingCreateRequest request) {
        this.customerId = request.getCustomerId();
        this.manualLiabilityDate = request.getDate();
    }

}
