package bg.energo.phoenix.model.response.receivable.latePaymentFine;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LatePaymentCustomerShortResponse {
    private Long id;
    private String personalNumber;
    private CustomerType type;
    private String displayName;

    public LatePaymentCustomerShortResponse(Long id, String  personalNumber, CustomerType type, String name, String middleName, String lastName, String legalFormName) {
        this.id = id;
        this.personalNumber = personalNumber;
        this.type = type;

        if (type.equals(CustomerType.LEGAL_ENTITY)) {
            this.displayName = String.format("%s (%s %s)", personalNumber, name, legalFormName);
        } else {
            String fullName = String.format("%s%s %s",
                    name,
                    StringUtils.isNotEmpty(middleName) ? " " + middleName : "",
                    lastName
            );
            this.displayName = String.format("%s (%s)", personalNumber, fullName.trim());
        }
    }

}
