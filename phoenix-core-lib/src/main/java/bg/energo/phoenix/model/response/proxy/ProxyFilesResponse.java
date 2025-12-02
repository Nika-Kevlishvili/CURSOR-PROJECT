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
public class ProxyFilesResponse {

    private Long id;
    private String name;
    private String fileUrl;
    private Long contractProxyId;
    private ContractSubObjectStatus status;

}
