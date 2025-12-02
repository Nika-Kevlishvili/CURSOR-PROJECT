package bg.energo.phoenix.model.request.contract;

import bg.energo.phoenix.model.customAnotations.contract.proxy.ProxyValidator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ProxyValidator
public class ProxyAddRequest {

    @NotNull(message = "basicParameters.proxy.foreignEntityPerson-[ForeignEntityPerson] can't be null;")
    private Boolean proxyForeignEntityPerson;

    @NotNull(message = "basicParameters.proxy.proxyName-[proxyName] is required;")
    @Size(min = 1, max = 512, message = "basicParameters.proxy.proxyName-[proxyName] size should be between {min} and {max} symbols;")
    @Pattern(regexp= "^[А-Яа-яA-Za-z0-9\\d–@#$&*()+\\-:.,'‘€№=\\s]*$", message = "basicParameters.proxy.proxyName-[proxyName] does not match allowed symbols;")
    private String proxyName;

    @NotNull(message = "basicParameters.proxy.proxyCustomerIdentifier-[ProxyCustomerIdentifier] is required;")
    @Size(min = 1, max = 32, message = "basicParameters.proxy.proxyCustomerIdentifier-[ProxyCustomerIdentifier] size should be between {min} and {max} symbols;")
    private String proxyCustomerIdentifier;

   /* @NotNull(message = "basicParameters.proxy.customerType-[proxyCustomerType] shouldn't be null;")
    private CustomerType proxyCustomerType;*/

    private String proxyEmail;

    private String proxyPhone;

    @Pattern(regexp= "^[0-9A-Za-zА-Яа-я–-]+$", message = "basicParameters.proxy.proxyPowerOfAttorneyNumber-[proxyPowerOfAttorneyNumber] does not match allowed symbols;")
    @Size(min = 1, max = 32, message = "basicParameters.proxy.proxyPowerOfAttorneyNumber-[authorizedProxyPowerOfAttorneyNumber] size should be between {min} and {max} symbols;")
    private String proxyPowerOfAttorneyNumber;

    @NotNull(message = "basicParameters.proxy.proxyData-[proxyData] shouldn't be null;")
    private LocalDate proxyData;

    //@NotNull(message = "basicParameters.proxy.proxyValidTill-[proxyValidTill] shouldn't be null;")
    private LocalDate proxyValidTill;

    @NotNull(message = "basicParameters.proxy.notaryPublic-[notaryPublic] shouldn't be null;")
    @Size(min = 1, max = 512, message = "basicParameters.proxy.notaryPublic-[notaryPublic] size should be between {min} and {max} symbols;")
    @Pattern(regexp= "^[А-Яа-яA-Za-z\\d–@#$&*()+-:.,‘€№=\\s]*$", message = "basicParameters.proxy.notaryPublic-[notaryPublic] does not match allowed symbols;")
    private String notaryPublic;

    //@Size(min = 1, max = 32, message = "basicParameters.proxy.registrationNumber-[registrationNumber] size should be between {min} and {max} symbols;")
    //@Pattern(regexp= "^[0-9A-Za-zА-Яа-я–-]+$", message = "basicParameters.proxy.proxyPowerOfAttorneyNumber-[proxyPowerOfAttorneyNumber] does not match allowed symbols;")
    private String registrationNumber;

    @NotNull(message = "basicParameters.proxy.areaOfOperation-[areaOfOperation] shouldn't be null;")
    @Size(min = 1, max = 512, message = "basicParameters.proxy.areaOfOperation-[areaOfOperation] size should be between {min} and {max} symbols;")
    //@Pattern(regexp= "^[А-ЯA-ZA-Za-z0-9@#$&*()–_+\\-§?!\\/\\\\<>:.,‘€№= \\r\\n\\s\\d]*$", message = "basicParameters.proxy.areaOfOperation-[areaOfOperation] does not match allowed symbols;")
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
