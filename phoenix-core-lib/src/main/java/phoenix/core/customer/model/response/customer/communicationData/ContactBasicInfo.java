package phoenix.core.customer.model.response.customer.communicationData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.customer.CustomerCommContactTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactBasicInfo {

    private Long id;

    private String name;

    private CustomerCommContactTypes contactType;

}
