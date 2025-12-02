package bg.energo.phoenix.model.response.nomenclature.crm;

import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailMailboxesResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String email;
    private boolean emailForSendingInvoices;
    private boolean emailForGridOperator;
    private boolean communicationForContract;

    public EmailMailboxesResponse(EmailMailboxes emailMailboxes) {
        this.id = emailMailboxes.getId();
        this.name = emailMailboxes.getName();
        this.orderingId = emailMailboxes.getOrderingId();
        this.defaultSelection = emailMailboxes.isDefaultSelection();
        this.status = emailMailboxes.getStatus();
        this.email = emailMailboxes.getEmailAddress();
        this.emailForSendingInvoices = emailMailboxes.isEmailForSendingInvoices();
        this.emailForGridOperator = emailMailboxes.isEmailForGridOperator();
        this.communicationForContract = emailMailboxes.isCommunicationForContract();
    }
}
