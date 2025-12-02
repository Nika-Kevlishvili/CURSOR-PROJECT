package bg.energo.phoenix.model.request.product.service;

import lombok.Data;

@Data
public class ServiceContractServiceListingRequest {

    private Long customerId;
    private String prompt;
    private Integer page;
    private Integer size;
}
