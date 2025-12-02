package bg.energo.phoenix.model.entity.pod.pod;


import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "pod", schema = "pod")
public class PointOfDelivery extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "pod_id_seq",
            sequenceName = "pod.pod_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "pod_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PodStatus status;

    @Column(name = "identifier")
    private String identifier;
    @Column(name = "impossibility_disconnection")
    private Boolean impossibleToDisconnect;
    @Column(name = "blocked_for_disconnection")
    private Boolean blockedDisconnection;
    @Column(name = "blocked_for_disconnection_date_from")
    private LocalDate blockedDisconnectionDateFrom;
    @Column(name = "blocked_for_disconnection_date_to")
    private LocalDate blockedDisconnectionDateTo;
    @Column(name = "blocked_for_disconnection_reason")
    private String blockedDisconnectionReason;
    @Column(name = "blocked_for_disconnection_additional_info")
    private String blockedDisconnectionInfo;
    @Column(name = "blocked_for_billing")
    private Boolean blockedBilling;
    @Column(name = "blocked_for_billing_date_from")
    private LocalDate blockedBillingDateFrom;
    @Column(name = "blocked_for_billing_date_to")
    private LocalDate blockedBillingDateTo;
    @Column(name = "blocked_for_billing_reason")
    private String blockedBillingReason;
    @Column(name = "blocked_for_billing_additional_info")
    private String blockedBillingInfo;
    @Column(name = "grid_operator_id")
    private Long gridOperatorId;
    @Column(name = "last_pod_detail_id")
    private Long lastPodDetailId;
    @Column(name = "disconnected")
    private Boolean disconnectionPowerSupply;


}
