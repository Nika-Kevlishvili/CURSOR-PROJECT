package bg.energo.phoenix.model.entity.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.TypeOfFile;
import bg.energo.phoenix.model.enums.task.PerformerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "collection_channels", schema = "receivable")
public class CollectionChannel extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "collection_channels_id_seq",
            schema = "receivable",
            sequenceName = "collection_channels_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "collection_channels_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CollectionChannelType type;

    @Column(name = "notification_employee_id")
    private Long employeeId;

    @Column(name = "notification_tag_id")
    private Long tagId;

    @Column(name = "performer_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PerformerType performerType;

    @Column(name = "collection_partner_id")
    private Long collectionPartnerId;

    @Column(name = "income_account_number")
    private String numberOfIncomeAccount;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "customer_condition_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CustomerConditionType conditionType;

    @Column(name = "customer_conditions")
    private String condition;

    @Column(name = "list_of_customers")
    private String listOfCustomers;

    @Column(name = "exclude_liabilities_by_amount_less_than")
    private BigDecimal lessThan;

    @Column(name = "exclude_liabilities_by_amount_greater_than")
    private BigDecimal greaterThan;

    @Column(name = "file_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TypeOfFile typeOfFile;

    @Column(name = "global_bank")
    private Boolean isGlobalBank;

    @Column(name = "data_sending_schedule")
    private String dataSendingSchedule;

    @Column(name = "data_receiving_schedule")
    private String dataReceivingSchedule;

    @Column(name = "number_of_working_days_for_waiting_payment")
    private Integer numberOfWorkingDays;

    @Column(name = "calendar_id")
    private Long calendarId;

    @Column(name = "waiting_period_tolerance_in_hours")
    private Integer waitingPeriodToleranceInHours;

    @Column(name = "folder_for_file_receiving")
    private String folderForFileReceiving;

    @Column(name = "folder_for_file_sending")
    private String folderForFileSending;

    @Column(name = "email_for_file_sending")
    private String emailForFileSending;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "waiting_period_time")
    private LocalDateTime waitingPeriodTime;

    @Column(name = "combine_liabilities")
    private Boolean combineLiabilities;

}
