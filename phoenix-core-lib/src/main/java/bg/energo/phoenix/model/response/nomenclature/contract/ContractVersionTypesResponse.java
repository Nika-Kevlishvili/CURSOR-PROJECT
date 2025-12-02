package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.ContractVersionType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractVersionTypesResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private boolean isHardcoded;

    public ContractVersionTypesResponse(ContractVersionType contractVersionType) {
        this.id = contractVersionType.getId();
        this.name = contractVersionType.getName();
        this.orderingId = contractVersionType.getOrderingId();
        this.defaultSelection = contractVersionType.getIsDefault();
        this.status = contractVersionType.getStatus();
        this.isHardcoded = contractVersionType.getIsHardCoded();
    }
}
