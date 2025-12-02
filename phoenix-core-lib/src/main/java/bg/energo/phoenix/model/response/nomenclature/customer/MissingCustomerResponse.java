package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.MissingCustomer;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MissingCustomerResponse {

    private Long id;
    private String uic;
    private String name;
    private String nameTransliterated;
    private String legalForm;
    private String legalFormTransliterated;
    private Long orderingId;
    private Boolean defaultSelection;
    private NomenclatureItemStatus status;

    public MissingCustomerResponse(MissingCustomer missingCustomer) {
        this.id = missingCustomer.getId();
        this.uic = missingCustomer.getUic();
        this.name = missingCustomer.getName();
        this.nameTransliterated = missingCustomer.getNameTransliterated();
        this.legalForm = missingCustomer.getLegalForm();
        this.legalFormTransliterated = missingCustomer.getLegalFormTransliterated();
        this.orderingId = missingCustomer.getOrderingId();
        this.defaultSelection = missingCustomer.getIsDefault();
        this.status = missingCustomer.getStatus();
    }
}
