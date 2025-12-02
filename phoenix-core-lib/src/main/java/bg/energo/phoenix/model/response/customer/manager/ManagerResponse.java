package bg.energo.phoenix.model.response.customer.manager;

import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;

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
    private String birthDate;
    private Long representationMethodId;
    private String representationMethodName;
    private Long titleId;
    private String titleName;
    private String additionalInformation;
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
        this.birthDate = manager.getBirthDate() ==null ? null : manager.getBirthDate().toString();
        if(manager.getRepresentationMethod()!=null){
            this.representationMethodId = manager.getRepresentationMethod().getId();
            this.representationMethodName = manager.getRepresentationMethod().getName();
        }

        this.titleId = manager.getTitle().getId();
        this.titleName = manager.getTitle().getName();
        this.additionalInformation = manager.getAdditionalInfo();
        this.status = manager.getStatus();
        this.systemUserId = manager.getSystemUserId();
        this.customerDetailId = manager.getCustomerDetailId();
    }

}
