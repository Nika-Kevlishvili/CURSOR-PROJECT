package bg.energo.phoenix.service.product.price.priceComponentGroup;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroup;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroupDetails;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupPriceComponentResponse;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupResponse;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceComponentGroupMapper {

    public PriceComponentGroupResponse priceComponentGroupResponseFromEntity(PriceComponentGroup group,
                                                                             PriceComponentGroupDetails details,
                                                                             List<PriceComponentGroupVersion> versions,
                                                                             List<PriceComponentGroupPriceComponentResponse> priceComponentsList) {
        return PriceComponentGroupResponse.builder()
                .groupId(details.getPriceComponentGroupId())
                .groupDetailId(details.getId())
                .name(details.getName())
                .versionId(details.getVersionId())
                .status(group.getStatus())
                .versions(versions)
                .priceComponentsList(priceComponentsList)
                .build();
    }

    public PriceComponent copyPriceComponent(PriceComponent original) {
        PriceComponent copy = new PriceComponent();
        copy.setName(original.getName());
        copy.setInvoiceAndTemplateText(original.getInvoiceAndTemplateText());
        copy.setNumberType(original.getNumberType());
        copy.setGlobalVatRate(original.getGlobalVatRate());
        copy.setIncomeAccountNumber(original.getIncomeAccountNumber());
        copy.setCostCenterControllingOrder(original.getCostCenterControllingOrder());
        copy.setContractTemplateTag(original.getContractTemplateTag());
        copy.setPriceInWords(original.getPriceInWords());
        copy.setPriceFormula(original.getPriceFormula());
        copy.setStatus(original.getStatus());
        copy.setIssuedSeparateInvoice(original.getIssuedSeparateInvoice());
        copy.setConditions(original.getConditions());
        copy.setFormulaVariables(null);
        copy.setDiscount(original.getDiscount());
        return copy;
    }

    public List<PriceComponentFormulaVariable> copyFormulaVariables(List<PriceComponentFormulaVariable> original, PriceComponent priceComponent) {
        List<PriceComponentFormulaVariable> formulaVariables = new ArrayList<>();
        for (PriceComponentFormulaVariable item : original) {
            PriceComponentFormulaVariable priceComponentFormulaVariable = new PriceComponentFormulaVariable();
            priceComponentFormulaVariable.setVariable(item.getVariable());
            priceComponentFormulaVariable.setPriceComponent(priceComponent);
            priceComponentFormulaVariable.setDescription(item.getDescription());
            priceComponentFormulaVariable.setValue(item.getValue());
            priceComponentFormulaVariable.setValueFrom(item.getValueFrom());
            priceComponentFormulaVariable.setValueTo(item.getValueTo());
            priceComponentFormulaVariable.setProfileForBalancing(item.getProfileForBalancing());
            formulaVariables.add(priceComponentFormulaVariable);
        }
        return formulaVariables;
    }

    public PriceComponentGroupResponse toPriceComponentGroupViewResponse(PriceComponentGroup priceComponentGroup,
                                                                         PriceComponentGroupDetails priceComponentGroupDetails,
                                                                         List<PriceComponentGroupVersion> list,
                                                                         List<PriceComponentGroupPriceComponentResponse> priceComponentSimpleResponses) {
        return PriceComponentGroupResponse.builder()
                .groupId(priceComponentGroup.getId())
                .status(priceComponentGroup.getStatus())
                .name(priceComponentGroupDetails.getName())
                .groupDetailId(priceComponentGroupDetails.getId())
                .versionId(priceComponentGroupDetails.getVersionId())
                .versions(list)
                .priceComponentsList(priceComponentSimpleResponses)
                .build();
    }

    public PriceComponentGroupVersion versionMap(PriceComponentGroupDetails priceComponentGroupDetails) {
        return PriceComponentGroupVersion.builder()
                .version(priceComponentGroupDetails.getVersionId())
                .startDate(priceComponentGroupDetails.getStartDate())
                .build();
    }
}
