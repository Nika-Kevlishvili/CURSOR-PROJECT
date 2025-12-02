package bg.energo.phoenix.model.response.customer.customerAccountManager;

import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.CustomerAccountManager;
import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CustomerAccountManagerResponse {
    private Long id;
    private Long customerDetailId;
    private Long accountManagerId;
    private Long accountManagerTypeId;
    private String accountManagerTypeName;
    private Status status;
    private String name;
    private String userName;
    private String firstName;
    private String lastName;
    private String displayName;
    private String email;
    private String organizationalUnit;
    private String businessUnit;
    private String systemUserId;
    private LocalDateTime createDate;
    private String modifySystemUserId;
    private LocalDateTime modifyDate;

    public CustomerAccountManagerResponse(AccountManager accountManager, CustomerAccountManager customerAccountManager) {
        this.id = customerAccountManager.getId();
        this.customerDetailId = customerAccountManager.getCustomerDetail().getId();
        this.accountManagerId = customerAccountManager.getManagerId();
        this.accountManagerTypeId = customerAccountManager.getAccountManagerType().getId();
        this.accountManagerTypeName = customerAccountManager.getAccountManagerType().getName();
        this.status = customerAccountManager.getStatus();
        this.systemUserId = customerAccountManager.getSystemUserId();
        this.createDate = customerAccountManager.getCreateDate();
        this.modifyDate = customerAccountManager.getModifyDate();
        this.modifySystemUserId = customerAccountManager.getModifySystemUserId();

        this.userName = accountManager.getUserName();
        this.firstName = accountManager.getFirstName();
        this.lastName = accountManager.getLastName();
        this.displayName = accountManager.getDisplayName();
        this.email = accountManager.getEmail();
        this.organizationalUnit = accountManager.getOrganizationalUnit();
        this.businessUnit = accountManager.getBusinessUnit();
        this.name = accountManager.getDisplayName() + " (" + accountManager.getUserName() + ")";
    }
}
