package phoenix.core.customer.model.response.customer.UnwantedCustomer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.entity.customer.UnwantedCustomer;
import phoenix.core.customer.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnwantedCustomerResponse {
    private Long id;
    private String identifier;
    private String name;
    private Long unwantedCustomerReasonId;
    private String unwantedCustomerReasonName;
    private String additionalInfo;
    private Boolean createContractRestriction;
    private Boolean createOrderRestriction;
    private String systemUserid;
    private UnwantedCustomerStatus status;
    private Date createDate;
    private Date modifyDate;
    private String modifySystemUserId;


    public UnwantedCustomerResponse(UnwantedCustomer unwantedCustomer, String reasonName) {
        this.id = unwantedCustomer.getId();
        this.identifier = unwantedCustomer.getIdentifier();
        this.name = unwantedCustomer.getName();
        this.unwantedCustomerReasonId = unwantedCustomer.getUnwantedCustomerReasonId();
        this.unwantedCustomerReasonName = reasonName;
        this.additionalInfo = unwantedCustomer.getAdditionalInfo();
        this.createContractRestriction = unwantedCustomer.getCreateContractRestriction();
        this.createOrderRestriction = unwantedCustomer.getCreateOrderRestriction();
        this.systemUserid = unwantedCustomer.getSystemUserid();
        this.status = unwantedCustomer.getStatus();
        this.createDate = unwantedCustomer.getCreateDate();
        this.modifyDate = unwantedCustomer.getModifyDate();
        this.modifySystemUserId = unwantedCustomer.getModifySystemUserId();
    }
}
