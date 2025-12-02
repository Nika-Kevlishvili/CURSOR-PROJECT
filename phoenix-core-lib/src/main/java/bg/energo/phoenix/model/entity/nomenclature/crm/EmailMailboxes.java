package bg.energo.phoenix.model.entity.nomenclature.crm;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.crm.EmailMailboxesRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Setter
@Getter
@Table(name = "email_mailboxes", schema = "nomenclature")
public class EmailMailboxes extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "email_mailboxes_id_seq",
            sequenceName = "nomenclature.email_mailboxes_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_mailboxes_id_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "email_for_sending_invoices")
    private boolean emailForSendingInvoices;

    @Column(name = "email_for_grid_operator")
    private boolean emailForGridOperator;

    @Column(name = "communication_for_contract")
    private boolean communicationForContract;

    @Column(name = "is_hard_coded")
    private Boolean isHardCoded;

    public EmailMailboxes(EmailMailboxesRequest request) {
        this.name = request.getName();
        this.status = request.getStatus();
        this.emailAddress = request.getEmailAddress();
        this.emailForSendingInvoices = request.isEmailForSendingInvoices();
        this.emailForGridOperator = request.isEmailForGridOperator();
        this.communicationForContract = request.isCommunicationForContract();
    }
}
