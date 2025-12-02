package bg.energo.phoenix.model.response.contract.order.goods;

import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoodsOrderProxyResponse {
    private Long id;
    private Boolean proxyForeignEntityPerson;
    private String proxyName;
    private String proxyCustomerIdentifier;
    private String proxyEmail;
    private String proxyPhone;
    private String proxyPowerOfAttorneyNumber;
    private LocalDate proxyData;
    private LocalDate proxyValidTill;
    private String notaryPublic;
    private String registrationNumber;
    private String areaOfOperation;
    //Authorized Proxy
    private Boolean authorizedProxyForeignEntityPerson;
    private String proxyAuthorizedByProxy;
    private String authorizedProxyCustomerIdentifier;
    private CustomerType authorizedProxyCustomerType;
    private String authorizedProxyEmail;
    private String authorizedProxyPhone;
    private String authorizedProxyPowerOfAttorneyNumber;
    private LocalDate authorizedProxyData;
    private LocalDate authorizedProxyValidTill;
    private String authorizedProxyNotaryPublic;
    private String authorizedProxyRegistrationNumber;
    private String authorizedProxyAreaOfOperation;
    private ContractSubObjectStatus status;

    private List<GoodsOrderProxyFilesResponse> proxyFiles;
    private List<GoodsOrderProxyManagersResponse> proxyManagers;
}
