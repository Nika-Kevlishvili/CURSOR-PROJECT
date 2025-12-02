package phoenix.core.customer.model.request.communicationData.contactPerson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateContactPersonRequest extends BaseContactPersonRequest {

    public CreateContactPersonRequest(EditContactPersonRequest editContactPersonRequest) {
        this.setName(editContactPersonRequest.getName());
        this.setMiddleName(editContactPersonRequest.getMiddleName());
        this.setSurname(editContactPersonRequest.getSurname());
        this.setStatus(editContactPersonRequest.getStatus());
        this.setJobPosition(editContactPersonRequest.getJobPosition());
        this.setPositionHeldFrom(editContactPersonRequest.getPositionHeldFrom());
        this.setPositionHeldTo(editContactPersonRequest.getPositionHeldTo());
        this.setBirthDate(editContactPersonRequest.getBirthDate());
        this.setTitleId(editContactPersonRequest.getTitleId());
        this.setAdditionalInformation(editContactPersonRequest.getAdditionalInformation());
    }

}
