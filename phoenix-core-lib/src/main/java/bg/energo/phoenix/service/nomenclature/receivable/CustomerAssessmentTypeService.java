package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.model.request.nomenclature.receivable.CustomerAssessmentTypeFilterRequest;
import bg.energo.phoenix.model.response.nomenclature.receivable.AdditionalConditionShortResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.CustomerAssessmentTypeShortResponse;
import bg.energo.phoenix.repository.nomenclature.receivable.CustomerAssessmentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAssessmentTypeService {

    private final CustomerAssessmentTypeRepository customerAssessmentTypeRepository;

    public List<CustomerAssessmentTypeShortResponse> findAllTypes() {
        log.debug("Filtering customer assessment type list");
        return customerAssessmentTypeRepository.findAllTypes().stream().map(CustomerAssessmentTypeShortResponse::new).toList();
    }

    public List<AdditionalConditionShortResponse> findAllAdditionalConditionsByType(CustomerAssessmentTypeFilterRequest request) {
        log.debug("Filtering customer assessment with type : %s".formatted(request.getTypeId()));
        return customerAssessmentTypeRepository.findAllByTypeId(request.getTypeId());
    }


}
