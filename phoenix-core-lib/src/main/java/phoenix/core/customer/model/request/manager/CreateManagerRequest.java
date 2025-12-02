package phoenix.core.customer.model.request.manager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CreateManagerRequest extends BaseManagerRequest {

    public CreateManagerRequest(EditManagerRequest editManagerRequest) {
        super(
                editManagerRequest.getTitleId(),
                editManagerRequest.getName(),
                editManagerRequest.getMiddleName(),
                editManagerRequest.getSurname(),
                editManagerRequest.getPersonalNumber(),
                editManagerRequest.getJobPosition(),
                editManagerRequest.getPositionHeldFrom(),
                editManagerRequest.getPositionHeldTo(),
                editManagerRequest.getBirthDate(),
                editManagerRequest.getRepresentationMethodId(),
                editManagerRequest.getAdditionalInformation(),
                editManagerRequest.getStatus()
        );
    }
    public static List<CreateManagerRequest> getCreateManagersRequestList(List<EditManagerRequest> editManagerRequest) {
        List<CreateManagerRequest> requestList = new ArrayList<>();
        for (int i = 0; i < editManagerRequest.size(); i++) {
            requestList.add(new CreateManagerRequest(editManagerRequest.get(i)));
        }
        return requestList;
    }
}
