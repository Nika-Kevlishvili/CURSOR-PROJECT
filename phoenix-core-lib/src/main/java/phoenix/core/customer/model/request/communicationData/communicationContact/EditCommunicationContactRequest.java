package phoenix.core.customer.model.request.communicationData.communicationContact;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditCommunicationContactRequest extends BaseCommunicationContactRequest {

    private Long id;

}
