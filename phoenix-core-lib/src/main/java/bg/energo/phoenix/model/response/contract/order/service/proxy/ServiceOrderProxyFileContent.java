package bg.energo.phoenix.model.response.contract.order.service.proxy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderProxyFileContent {
    private String name;
    private byte[] content;
}
