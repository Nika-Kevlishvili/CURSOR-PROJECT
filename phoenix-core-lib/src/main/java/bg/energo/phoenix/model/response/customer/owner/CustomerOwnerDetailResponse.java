package bg.energo.phoenix.model.response.customer.owner;

import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerOwner;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalForm;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOwnerDetailResponse {
    private Long id;
    private String personalNumber;
    private String name;
    private CustomerType ownerType;
    private String additionalInformation;
    private Long belongingOwnerCapitalId;
    private String belongingOwnerCapitalName;

    public CustomerOwnerDetailResponse(CustomerOwner owner, CustomerDetails details, LegalForm legalForm) {
        this.id = owner.getId();

        Customer ownerCustomer = owner.getOwnerCustomer();
        if (ownerCustomer.getCustomerType().equals(CustomerType.LEGAL_ENTITY)) {
            String legalFormName = "";
            if (legalForm != null && legalForm.getName() != null) {
                legalFormName = legalForm.getName();
            }
            this.name = ownerCustomer.getIdentifier()
                    .concat("(" + details.getName())
                    .concat(" " + legalFormName)
                    .concat(")");
        } else {
            this.name = String.format(
                    "%s %s %s",
                    details.getName(),
                    StringUtils.isNotEmpty(details.getMiddleName()) ? details.getMiddleName() : "",
                    StringUtils.isNotEmpty(details.getLastName()) ? details.getLastName() : ""
            ).trim();
        }

        this.personalNumber = ownerCustomer.getIdentifier();
        this.additionalInformation = owner.getAdditionalInfo();
        this.belongingOwnerCapitalId = owner.getBelongingCapitalOwner().getId();
        this.belongingOwnerCapitalName = owner.getBelongingCapitalOwner().getName();
        this.ownerType = ownerCustomer.getCustomerType();
    }
}
