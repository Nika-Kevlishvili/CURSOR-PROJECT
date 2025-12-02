package bg.energo.phoenix.permissions;

import bg.energo.common.security.acl.definitions.AclValue;
import lombok.Getter;

public enum SubPermissionEnum {

    CUSTOMER_VIEW_BASIC("app.demo.cust.partial_view.basic",
            "acl_values.customers.view_partial.basic_info",
            true),
    CUSTOMER_VIEW_PRODUCTS("app.demo.cust.partial_view.products",
            "acl_values.customers.view_partial.products_info",
            true);

    @Getter
    private String key;
    @Getter
    private String value;
    @Getter
    private boolean translatable;

    SubPermissionEnum(String key, String value, boolean translatable) {
        this.key = key;
        this.value = value;
        this.translatable = translatable;
    }

    public AclValue getAclValue() {
        return new AclValue(key, value, true);
    }
}
