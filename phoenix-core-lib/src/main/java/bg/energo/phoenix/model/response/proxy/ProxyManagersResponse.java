package bg.energo.phoenix.model.response.proxy;

import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProxyManagersResponse {

    private Long id;
    private String managerName;
    private String managerMiddleName;
    private String managerSurName;
    private Long contractProxyId;
    private Long customerManagerId;
    private ContractSubObjectStatus status;

}
