package phoenix.core.customer.model.response.customer.communicationData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommContactPerson;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactPersonBasicInfo {

    private Long id;

    private String name;

    public ContactPersonBasicInfo(CustomerCommContactPerson contactPerson) {
        this.id = contactPerson.getId();
        this.name = contactPerson.getName()
                .concat(StringUtils.isEmpty(contactPerson.getMiddleName()) ? "" : " " + contactPerson.getMiddleName())
                .concat(" ")
                .concat(contactPerson.getSurname())
                .concat(" ")
                .concat("(" + contactPerson.getJobPosition() + ")");
    }

}
