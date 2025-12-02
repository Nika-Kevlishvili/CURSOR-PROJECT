package bg.energo.phoenix.model.response.contract.order.service.proxy;

import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderProxyFile;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceOrderProxyFileResponse {
    private Long id;
    private String name;

    public ServiceOrderProxyFileResponse(ServiceOrderProxyFile file) {
        this.id = file.getId();
        String name = file.getName();
        try {
            this.name = name.substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (IndexOutOfBoundsException e) {
            this.name = name;
        }
    }
}
