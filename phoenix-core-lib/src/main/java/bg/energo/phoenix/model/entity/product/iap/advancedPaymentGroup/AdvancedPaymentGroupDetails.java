package bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "interim_advance_payment_group_details", schema = "interim_advance_payment")
public class AdvancedPaymentGroupDetails extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "interim_advance_payment_group_details_id_seq",
            sequenceName = "interim_advance_payment.interim_advance_payment_group_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interim_advance_payment_group_details_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "interim_advance_payment_group_id")
    private Long advancedPaymentGroupId;

    @Column(name = "version_id")
    private Long versionId;
}
