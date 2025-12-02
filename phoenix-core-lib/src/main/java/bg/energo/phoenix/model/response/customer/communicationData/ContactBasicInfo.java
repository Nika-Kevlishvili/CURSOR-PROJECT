package bg.energo.phoenix.model.response.customer.communicationData;

import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactBasicInfo {

    private Long id;

    private String name;

    private CustomerCommContactTypes contactType;

}
