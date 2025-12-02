package bg.energo.phoenix.model.request.contract.order.service.proxy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderProxyUpdateRequest extends ServiceOrderProxyBaseRequest {

    private Long id;

}
