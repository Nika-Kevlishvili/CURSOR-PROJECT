package bg.energo.phoenix.model.response.customer.UnwantedCustomer;

import bg.energo.phoenix.model.entity.customer.UnwantedCustomer;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Param {@link #id} unique id of the unwanted customer
 * @Param {@link #identifier} identification number for the unwanted customer
 * @Param {@link #name} name of unwanted customer
 * @Param {@link #unwantedCustomerReasonId} unwantedCustomerReason nomenclature id
 * @Param {@link #unwantedCustomerReasonName} unwantedCustomerReasonName nomenclature name
 * @Param {@link #additionalInfo} String for additional info text
 * @Param {@link #createContractRestriction} boolean value for contract creation restriction
 * @Param {@link #createOrderRestriction} boolean value for order creation restriction
 * @Param {@link #systemUserid} system user id
 * @Param {@link #status} unwanted customer status
 * @Param {@link #createDate} date when customer was created
 * @Param {@link #modifyDate} date when customer was modified
 * @Param {@link #modifySystemUserId} system user id who modified the record
 *
 */
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
    private LocalDateTime createDate;
    private LocalDateTime modifyDate;
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
        this.systemUserid = unwantedCustomer.getSystemUserId();
        this.status = unwantedCustomer.getStatus();
        this.createDate = unwantedCustomer.getCreateDate();
        this.modifyDate = unwantedCustomer.getModifyDate();
        this.modifySystemUserId = unwantedCustomer.getModifySystemUserId();
    }
}
