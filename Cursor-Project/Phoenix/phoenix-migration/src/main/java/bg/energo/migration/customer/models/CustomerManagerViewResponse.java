package bg.energo.migration.customer.models;

import bg.energo.migration.customer.entity.manager.CustomerManagerView;
import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import lombok.Data;

@Data
public class CustomerManagerViewResponse {
    private  String personalNumber;
    private String customerId;
    private String name;
    private String middleName;
    private String surname;
    private Long titleId;
    private Long representationMethodId;
    private String jobPosition;
    private Status status;

    public CustomerManagerViewResponse(CustomerManagerViewMiddleResponse manager) {
        this.personalNumber = manager.getPersonalNumber();
        this.customerId = manager.getCustomerId();
        this.name = manager.getName();
        this.middleName = manager.getMiddleName();
        this.surname = manager.getSurname();
        this.titleId = manager.getTitleId();
        this.representationMethodId = manager.getRepresentationMethodId();
        this.jobPosition = manager.getJobPosition();
        this.status = manager.getStatus();
    }

    public CustomerManagerViewResponse(String personalNumber, String customerId, String name, String middleName, String surname, Long titleId, Long representationMethodId, String jobPosition, Status status) {
        this.personalNumber = personalNumber;
        this.customerId = customerId;
        this.name = name;
        this.middleName = middleName;
        this.surname = surname;
        this.titleId = titleId;
        this.representationMethodId = representationMethodId;
        this.jobPosition = jobPosition;
        this.status = status;
    }
}
