package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * <h1>CustomerShortResponse</h1>
 * {@link #id} customerId
 * {@link #personalNumber} personalNumber
 * {@link #type} {@link CustomerType} type
 * {@link #name} name
 */
@Data
public class CustomerShortResponse {

    private Long id;
    private String personalNumber;
    private CustomerType type;
    private Boolean businessActivity;
    private String name;
    private String legalFormName;


    public CustomerShortResponse(Long id, String personalNumber, CustomerType type,  Boolean businessActivity,String legalFormName, String name, String middleName, String lastName) {
        this.id = id;
        this.personalNumber = personalNumber;
        if (type.equals(CustomerType.LEGAL_ENTITY)) {
            this.name = personalNumber
                    .concat("(" + name)
                    .concat(" " + legalFormName)
                    .concat(")");
        } else {
            this.name = name
                    .concat(StringUtils.isEmpty(middleName) ? "" : " " + middleName)
                    .concat(" ")
                    .concat(lastName);
        }
        this.type = type;
        this.businessActivity = businessActivity;
        this.legalFormName=legalFormName;
    }
}
