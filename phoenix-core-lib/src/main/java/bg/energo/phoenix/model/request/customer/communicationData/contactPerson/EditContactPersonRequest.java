package bg.energo.phoenix.model.request.customer.communicationData.contactPerson;

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
