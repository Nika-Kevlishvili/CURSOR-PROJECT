package bg.energo.phoenix.model.response.contract.order.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceOrderServiceVersionResponse {
    private Long id; // this is the ID of the version
    private String name;
    private Long serviceId; // this is the ID of the parent
    private Long versionId;
}
