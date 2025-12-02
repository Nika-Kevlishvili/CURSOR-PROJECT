package phoenix.core.customer.model.request.communicationData.contactPerson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditContactPersonRequest extends BaseContactPersonRequest {

    private Long id;

}
