package bg.energo.phoenix.model.response.customer.customerAccountManager;

import bg.energo.phoenix.model.entity.customer.AccountManager;

public record AccountManagerShortResponse(Long id, String name, String username) {
    public AccountManagerShortResponse(AccountManager accountManager) {
        this(accountManager.getId(), accountManager.getDisplayName(), accountManager.getUserName());
    }
}
