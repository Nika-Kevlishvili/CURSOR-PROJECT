package phoenix.core.customer.model.response.customer.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import phoenix.core.customer.model.entity.customer.Manager;
import phoenix.core.customer.model.enums.customer.Status;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ManagerResponse {

    private Long id;
    private String name;
    private String middleName;
    private String surname;
    private String personalNumber;
    private String jobPosition;
    private LocalDate positionHeldFrom;
    private LocalDate positionHeldTo;
    private LocalDate birthDate;
    private Long representationMethodId;
    private String representationMethodName;
    private Long titleId;
    private String titleName;
    private String additionalInfo;
    private Status status;
    private String systemUserId;
    private Long customerDetailId;

    public ManagerResponse(Manager manager) {
        this.id = manager.getId();
        this.name = manager.getName();
        this.middleName = manager.getMiddleName();
        this.surname = manager.getSurname();
        this.personalNumber = manager.getPersonalNumber();
        this.jobPosition = manager.getJobPosition();
        this.positionHeldFrom = manager.getPositionHeldFrom();
        this.positionHeldTo = manager.getPositionHeldTo();
        this.birthDate = manager.getBirthDate();
        this.representationMethodId = manager.getRepresentationMethod().getId();
        this.representationMethodName = manager.getRepresentationMethod().getName();
        this.titleId = manager.getTitle().getId();
        this.titleName = manager.getTitle().getName();
        this.additionalInfo = manager.getAdditionalInfo();
        this.status = manager.getStatus();
        this.systemUserId = manager.getSystemUserId();
        this.customerDetailId = manager.getCustomerDetailId();
    }

}
