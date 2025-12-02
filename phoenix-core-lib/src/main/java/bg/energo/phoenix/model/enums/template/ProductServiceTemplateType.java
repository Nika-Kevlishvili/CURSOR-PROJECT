package bg.energo.phoenix.model.enums.template;

import lombok.Getter;

import java.util.List;

@Getter
public enum ProductServiceTemplateType {
    CONTRACT_TEMPLATE(List.of(ContractTemplateLanguage.BULGARIAN),List.of(ContractTemplateType.DOCUMENT)),
    BI_CONTRACT_TEMPLATE(List.of(ContractTemplateLanguage.BILINGUAL),List.of(ContractTemplateType.DOCUMENT)),
    INVOICE_TEMPLATE(List.of(ContractTemplateLanguage.BULGARIAN, ContractTemplateLanguage.BILINGUAL),List.of(ContractTemplateType.DOCUMENT)),
    ADDITIONAL_INVOICE_TEMPLATE(List.of(ContractTemplateLanguage.BULGARIAN, ContractTemplateLanguage.BILINGUAL),List.of(ContractTemplateType.DOCUMENT)),
    EMAIL_TEMPLATE(List.of(ContractTemplateLanguage.BULGARIAN,ContractTemplateLanguage.BILINGUAL),List.of(ContractTemplateType.EMAIL));

    private final List<ContractTemplateLanguage> language;
    private final List<ContractTemplateType> templateTypes;

    ProductServiceTemplateType(List<ContractTemplateLanguage> language,List<ContractTemplateType> templateTypes) {
        this.language = language;
        this.templateTypes = templateTypes;
    }

    public ContractTemplatePurposes getPurpose(ContractTemplatePurposes purposes){
        if(this.equals(INVOICE_TEMPLATE)||this.equals(ADDITIONAL_INVOICE_TEMPLATE)){
            return ContractTemplatePurposes.INVOICE;
        }
        return purposes;
    }
}
