package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.customAnotations.product.service.ValidServiceTerms;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.CreateServiceContractTermRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ValidServiceTerms
public class CreateServiceRequest extends BaseServiceRequest {

    private List<@Valid CreateServiceContractTermRequest> contractTerms;

}
