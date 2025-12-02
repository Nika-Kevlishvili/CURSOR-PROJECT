package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.customAnotations.contract.order.goods.request.GoodsOrderProxyValidator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
@GoodsOrderProxyValidator
public class GoodsOrderProxyAddRequest {
    @NotNull(message = "basicParameters.goodsOrderProxyAddRequest.foreignEntityPerson-[ForeignEntityPerson] can't be null;")
    private Boolean proxyForeignEntityPerson;

    @NotNull(message = "basicParameters.goodsOrderProxyAddRequest.proxyName-[proxyName] is required;")
    @Size(min = 1, max = 512, message = "basicParameters.goodsOrderProxyAddRequest.proxyName-[proxyName] size should be between {min} and {max} symbols;")
    @Pattern(regexp= "^[А-Яа-яA-Za-z0-9\\d–@#$&*()+\\-:.,'‘€№=\\s]*$", message = "basicParameters.proxy.proxyName-[proxyName] does not match allowed symbols;")
    private String proxyName;

    @NotNull(message = "basicParameters.goodsOrderProxyAddRequest.proxyCustomerIdentifier-[ProxyCustomerIdentifier] is required;")
    @Size(min = 1, max = 32, message = "basicParameters.goodsOrderProxyAddRequest.proxyCustomerIdentifier-[ProxyCustomerIdentifier] size should be between {min} and {max} symbols;")
    private String proxyCustomerIdentifier;

    private String proxyEmail;

    private String proxyPhone;

    @Pattern(regexp= "^[0-9A-Za-zА-Яа-я–-]+$", message = "basicParameters.goodsOrderProxyAddRequest.proxyPowerOfAttorneyNumber-[proxyPowerOfAttorneyNumber] does not match allowed symbols;")
    @Size(min = 1, max = 32, message = "basicParameters.goodsOrderProxyAddRequest.proxyPowerOfAttorneyNumber-[authorizedProxyPowerOfAttorneyNumber] size should be between {min} and {max} symbols;")
    private String proxyPowerOfAttorneyNumber;

    @NotNull(message = "basicParameters.goodsOrderProxyAddRequest.proxyData-[proxyData] shouldn't be null;")
    private LocalDate proxyData;

    private LocalDate proxyValidTill;

    @NotNull(message = "basicParameters.goodsOrderProxyAddRequest.notaryPublic-[notaryPublic] shouldn't be null;")
    @Size(min = 1, max = 512, message = "basicParameters.goodsOrderProxyAddRequest.notaryPublic-[notaryPublic] size should be between {min} and {max} symbols;")
    @Pattern(regexp= "^[А-Яа-яA-Za-z\\d–@#$&*()+-:.,‘€№=\\s]*$", message = "basicParameters.proxy.notaryPublic-[notaryPublic] does not match allowed symbols;")
    private String notaryPublic;

    private String registrationNumber;

    @NotNull(message = "basicParameters.goodsOrderProxyAddRequest.areaOfOperation-[areaOfOperation] shouldn't be null;")
    @Size(min = 1, max = 512, message = "basicParameters.goodsOrderProxyAddRequest.areaOfOperation-[areaOfOperation] size should be between {min} and {max} symbols;")
    @Pattern(regexp= "^[А-Яа-яA-Za-z\\d–@#$&*()+-:.,‘€№=\\s]*$", message = "basicParameters.proxy.areaOfOperation-[areaOfOperation] does not match allowed symbols;")
    private String areaOfOperation;

    //Authorized Proxy
    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyForeignEntityPerson-[authorizedProxyForeignEntityPerson] can't be null;")
    private Boolean authorizedProxyForeignEntityPerson;

    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.proxyAuthorizedByProxy-[proxyAuthorizedByProxy] is mandatory;")
    //@Size(min = 1, max = 512, message = "basicParameters.goodsOrderProxyAddRequest.proxyAuthorizedByProxy-[proxyAuthorizedByProxy] size should be between {min} and {max} symbols;")
    //@Pattern(regexp= "^[А-Яа-яA-Za-z0-9\\d–@#$&*()+\\-:.,'‘€№=\\s]*$", message = "basicParameters.proxy.proxyAuthorizedByProxy-[proxyAuthorizedByProxy] does not match allowed symbols;")
    private String proxyAuthorizedByProxy;

    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyCustomerIdentifier-[authorizedProxyCustomerIdentifier] is required;")
    //@Size(min = 1, max = 32, message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyCustomerIdentifier-[authorizedProxyCustomerIdentifier] size should be between {min} and {max} symbols;")
    private String authorizedProxyCustomerIdentifier;

    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyCustomerType-[authorizedProxyCustomerType] shouldn't be null;")
    /*private CustomerType authorizedProxyCustomerType;*/

    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] shouldn't be null;")
    //@Size(min = 1, max = 512, message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] size should be between {min} and {max} symbols;")
    private String authorizedProxyEmail;

    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] shouldn't be null;")
    //@Size(min = 1, max = 512, message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] size should be between {min} and {max} symbols;")
    private String authorizedProxyPhone;

    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyPowerOfAttorneyNumber-[authorizedProxyPowerOfAttorneyNumber] shouldn't be null;")
    //@Pattern(regexp= "^[0-9A-Za-zА-Яа-я–-]+$", message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyPowerOfAttorneyNumber-[authorizedProxyPowerOfAttorneyNumber] does not match allowed symbols;")
    //@Size(min = 1, max = 32, message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyPowerOfAttorneyNumber-[authorizedProxyPowerOfAttorneyNumber] size should be between {min} and {max} symbols;")
    private String authorizedProxyPowerOfAttorneyNumber;

    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyData-[authorizedProxyData] shouldn't be null;")
    private LocalDate authorizedProxyData;

    //@NotNull(message = "basicParameters.proxy.proxyData-[proxyValidTill] shouldn't be null;")
    private LocalDate authorizedProxyValidTill;

    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyNotaryPublic-[authorizedProxyNotaryPublic] shouldn't be null;")
    //@Size(min = 1, max = 512, message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyNotaryPublic-[authorizedProxyNotaryPublic] size should be between {min} and {max} symbols;")
    //@Pattern(regexp= "^[А-Яа-яA-Za-z0-9\\d–@#$&*()+\\-:.,'‘€№=\\s]*$", message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyNotaryPublic-[authorizedProxyNotaryPublic] does not match allowed symbols;")
    private String authorizedProxyNotaryPublic;

    //@Size(min = 1, max = 32, message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyRegistrationNumber-[authorizedProxyRegistrationNumber] size should be between {min} and {max} symbols;")
    //@Pattern(regexp= "^[0-9A-Za-zА-Яа-я–-]+$", message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyRegistrationNumber-[authorizedProxyRegistrationNumber] does not match allowed symbols;")
    private String authorizedProxyRegistrationNumber;

    //@NotNull(message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyAreaOfOperation-[authorizedProxyAreaOfOperation] shouldn't be null;")
    //@Size(min = 1, max = 512, message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyAreaOfOperation-[authorizedProxyAreaOfOperation] size should be between {min} and {max} symbols;")
    //@Pattern(regexp= "^[А-Яа-яA-Za-z0-9\\d–@#$&*()+\\-:.,'‘€№=\\s]*$", message = "basicParameters.goodsOrderProxyAddRequest.authorizedProxyAreaOfOperation-[authorizedProxyAreaOfOperation] does not match allowed symbols;")
    private String authorizedProxyAreaOfOperation;

    private Set<Long> fileIds;

    //@Size(min = 1, message = "basicParameters.proxy.managers-[managers] at least {min} manager is required;")
    private Set<Long> managerIds;
}

