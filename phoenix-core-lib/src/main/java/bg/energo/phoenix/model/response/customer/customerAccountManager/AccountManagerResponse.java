package bg.energo.phoenix.model.response.customer.customerAccountManager;

import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountManagerResponse {
    private Long id;
    private String name;
    private String userName;
    private String firstName;
    private String lastName;
    private String displayName;
    private String email;
    private String organizationalUnit;
    private String businessUnit;
    private Status status;
    private LocalDateTime createDate;

    public AccountManagerResponse(AccountManager accountManager) {
        this.id = accountManager.getId();
        this.userName = accountManager.getUserName();
        this.firstName = accountManager.getFirstName();
        this.lastName = accountManager.getLastName();
        this.displayName = accountManager.getDisplayName();
        this.email = accountManager.getEmail();
        this.organizationalUnit = accountManager.getOrganizationalUnit();
        this.businessUnit = accountManager.getBusinessUnit();
        this.status = accountManager.getStatus();
        this.createDate = accountManager.getCreateDate();
    }
}
