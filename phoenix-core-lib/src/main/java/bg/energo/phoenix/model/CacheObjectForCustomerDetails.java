package bg.energo.phoenix.model;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheObjectForCustomerDetails extends CacheObjectForDetails{

    private CustomerType customerType;
    private Boolean isBusiness;

    public CacheObjectForCustomerDetails(Long id, Long versionId, Long detailsId, CustomerType customerType, Boolean isBusiness) {
        super(id, versionId, detailsId);
        this.customerType = customerType;
        this.isBusiness = isBusiness;
    }
}
