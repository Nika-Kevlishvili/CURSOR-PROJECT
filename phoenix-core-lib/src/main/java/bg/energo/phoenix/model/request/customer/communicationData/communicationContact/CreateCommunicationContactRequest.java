package bg.energo.phoenix.model.request.customer.communicationData.communicationContact;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateCommunicationContactRequest extends BaseCommunicationContactRequest {

    public CreateCommunicationContactRequest(EditCommunicationContactRequest editCommunicationDataRequest) {
        this.setContactType(editCommunicationDataRequest.getContactType());
        this.setContactValue(editCommunicationDataRequest.getContactValue());
        this.setStatus(editCommunicationDataRequest.getStatus());
        this.setSendSms(editCommunicationDataRequest.getSendSms());
        this.setPlatformId(editCommunicationDataRequest.getPlatformId());
    }

}
