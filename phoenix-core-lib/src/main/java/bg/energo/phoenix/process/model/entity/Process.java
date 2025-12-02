package bg.energo.phoenix.process.model.entity;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.process.model.ProcessTransactionListener;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.model.enums.ProcessType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "process", schema = "process_management")

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(ProcessTransactionListener.class)
public class Process extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    @SequenceGenerator(
            name = "process_id_seq",
            sequenceName = "process_management.process_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProcessStatus status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProcessType type;

    @Column(name = "process_start_date")
    private LocalDateTime processStartDate;

    @Column(name = "process_complete_date")
    private LocalDateTime processCompleteDate;

    // TODO: 08.02.23 to be added later for other processes
//    postponedStart; ignoreErrorsAtRuntime; ignoreWarningsAtRuntime; startAfterProcess; incompatibleProcesses;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "user_permissions")
    private String userPermissions;

    @Column(name = "date_field")
    private LocalDate date;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ProcessedRecordInfo.class)
    @JoinColumn(name = "process_id")
    private List<ProcessedRecordInfo> processedRecordInfos;

    @Column(name = "collection_channel_id")
    private Long collectionChannelId;

    @Column(name = "payment_package_id")
    private Long paymentPackageId;

    @Column(name = "reminder_id")
    private Long reminderId;

    @Column(name = "currency_from_collection_channel")
    private Boolean currencyFromCollectionChannel;
}
