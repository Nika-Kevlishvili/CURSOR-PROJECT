package bg.energo.phoenix.model.response.contract.serviceContract;

import bg.energo.phoenix.model.enums.contract.ContractType;
import lombok.Data;

@Data
public class SubObjectContractResponse {

    private Long id;
   // private EntityStatus status;
    private String contractNumber;
    private ContractType type;
   // private ServiceContractDetailStatus contractStatus;
   // private LocalDate statusModifyDate;
    //private ServiceContractDetailsSubStatus subStatus;

}
