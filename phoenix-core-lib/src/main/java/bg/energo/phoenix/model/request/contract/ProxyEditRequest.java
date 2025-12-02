package bg.energo.phoenix.model.request.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class ProxyEditRequest extends ProxyAddRequest {
    private Long id;
}
