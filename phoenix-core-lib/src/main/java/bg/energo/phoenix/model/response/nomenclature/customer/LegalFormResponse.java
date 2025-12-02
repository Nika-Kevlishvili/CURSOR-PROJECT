package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalForm;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor

public class LegalFormResponse {
    private Long id;
    private String name;

    private String description;

    private List<LegalFormTranResponse> legalFormsTransliterated;

    private Boolean defaultSelection;

    @JsonProperty("orderingId")
    private Long orderId;

    private NomenclatureItemStatus status;

    public LegalFormResponse(LegalForm legalForm) {
        this.id = legalForm.getId();
        this.name = legalForm.getName();
        this.description = legalForm.getDescription();
        this.defaultSelection = legalForm.getDefaultSelection();
        this.orderId = legalForm.getOrderingId();
        this.legalFormsTransliterated = legalForm.getLegalFormTransliterated().stream()
                .filter(x -> !x.getStatus().equals(NomenclatureItemStatus.DELETED))
                .sorted((first, second) -> {
                    if (Objects.equals(first.getId(), second.getId())) {
                        return 0;
                    }
                    return first.getId() > second.getId() ? 1 : -1;
                })
                .map(LegalFormTranResponse::new)
                .toList();
        this.status = legalForm.getStatus();
    }
}
