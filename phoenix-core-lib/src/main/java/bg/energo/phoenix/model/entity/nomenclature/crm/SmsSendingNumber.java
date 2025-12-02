package bg.energo.phoenix.model.entity.nomenclature.crm;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sms_sending_numbers", schema = "nomenclature")
public class SmsSendingNumber extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "sms_sending_numbers_id_seq",
            sequenceName = "nomenclature.sms_sending_numbers_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sms_sending_numbers_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "sms_number")
    private String smsNumber;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "is_hard_coded")
    private boolean isHardCoded;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;
}
