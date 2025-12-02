package bg.energo.phoenix.model.response.service;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ContractServiceFilterResponse {
    private Long id;
    private Long serviceId;
    private String name;
    private long version;

    public ContractServiceFilterResponse(Long id,Long serviceId, String name, Long version) {
        this.id = id;
        this.serviceId = serviceId;
        this.name = name;
        this.version = version;
    }
}
