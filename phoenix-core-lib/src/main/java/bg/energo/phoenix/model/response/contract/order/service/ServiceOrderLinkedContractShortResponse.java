package bg.energo.phoenix.model.response.contract.order.service;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceOrderLinkedContractShortResponse extends ServiceOrderSubObjectShortResponse {

    private LocalDateTime createDate;
    private String contractType;

    public ServiceOrderLinkedContractShortResponse(Long id, String name, LocalDateTime createDate, String contractType) {
        super(id, name);
        this.createDate = createDate;
        this.contractType = contractType;
    }

}
