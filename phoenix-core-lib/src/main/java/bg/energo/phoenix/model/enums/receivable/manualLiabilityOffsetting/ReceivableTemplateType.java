package bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import lombok.Getter;

@Getter
public enum ReceivableTemplateType {
    EMAIL(ContractTemplateType.EMAIL),
    DOCUMENT(ContractTemplateType.DOCUMENT),;

    private final ContractTemplateType templateType;

    ReceivableTemplateType(ContractTemplateType templateType) {
        this.templateType = templateType;
    }
}
