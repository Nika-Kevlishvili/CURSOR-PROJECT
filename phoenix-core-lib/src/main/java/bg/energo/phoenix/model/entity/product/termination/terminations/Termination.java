package bg.energo.phoenix.model.entity.product.termination.terminations;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.termination.terminations.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;

@Entity
@Table(name = "terminations", schema = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Termination extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "terminations_id_seq",
            sequenceName = "product.terminations_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "terminations_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "contract_clause_number")
    private String contractClauseNumber;

    @Column(name = "auto_termination")
    private Boolean autoTermination;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "auto_termination_from")
    private AutoTerminationFrom autoTerminationFrom;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "event")
    private TerminationEvent event;

    @Column(name = "notice_due")
    private Boolean noticeDue;

    @Column(name = "notice_due_value_min")
    private Integer noticeDueValueMin;

    @Column(name = "notice_due_value_max")
    private Integer noticeDueValueMax;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "calculate_from")
    private CalculateFrom calculateFrom;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "notice_due_type")
    private NoticeDueType noticeDueType;

    @Column(name = "auto_email_notification")
    private Boolean autoEmailNotification;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private TerminationStatus status;

    @OneToMany(mappedBy = "termination", cascade = CascadeType.ALL)
    private Set<TerminationNotificationChannel> terminationNotificationChannels;

    @Column(name = "termination_group_detail_id")
    private Long terminationGroupDetailId;

    @Column(name = "contract_template_id")
    private Long templateId;

    public Termination(Long id, String name, String contractClauseNumber, Boolean autoTermination, AutoTerminationFrom autoTerminationFrom, TerminationEvent event, Boolean noticeDue, Integer noticeDueValueMin, Integer noticeDueValueMax, CalculateFrom calculateFrom, NoticeDueType noticeDueType, Boolean autoEmailNotification, String additionalInfo, TerminationStatus status, Set<TerminationNotificationChannel> terminationNotificationChannels, Long terminationGroupDetailId) {
        this.id = id;
        this.name = name;
        this.contractClauseNumber = contractClauseNumber;
        this.autoTermination = autoTermination;
        this.autoTerminationFrom = autoTerminationFrom;
        this.event = event;
        this.noticeDue = noticeDue;
        this.noticeDueValueMin = noticeDueValueMin;
        this.noticeDueValueMax = noticeDueValueMax;
        this.calculateFrom = calculateFrom;
        this.noticeDueType = noticeDueType;
        this.autoEmailNotification = autoEmailNotification;
        this.additionalInfo = additionalInfo;
        this.status = status;
        this.terminationNotificationChannels = terminationNotificationChannels;
        this.terminationGroupDetailId = terminationGroupDetailId;
    }
}
