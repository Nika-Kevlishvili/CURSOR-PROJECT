package bg.energo.phoenix.service.nomenclature.contract.baseInterestRate;

import bg.energo.phoenix.model.entity.nomenclature.contract.BaseInterestRate;
import bg.energo.phoenix.model.request.nomenclature.contract.BaseInterestRateRequest;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.BaseInterestRateResponse;
import org.springframework.stereotype.Service;

@Service
public class BaseInterestRateMapper {

    public BaseInterestRateResponse responseFromEntity(BaseInterestRate baseInterestRate) {
        BaseInterestRateResponse response = new BaseInterestRateResponse();
        response.setId(baseInterestRate.getId());
        response.setName(baseInterestRate.getName());
        response.setStatus(baseInterestRate.getStatus());
        response.setDefaultSelection(baseInterestRate.isDefaultSelection());
        response.setOrderingId(baseInterestRate.getOrderingId());
        response.setDateFrom(baseInterestRate.getDateFrom());
        response.setPercentageRate(baseInterestRate.getPercentageRate());
        return response;
    }


    public NomenclatureResponse nomenclatureResponseFromEntity(BaseInterestRate baseInterestRate) {
        return new NomenclatureResponse(
                baseInterestRate.getId(),
                baseInterestRate.getName(),
                baseInterestRate.getOrderingId(),
                baseInterestRate.isDefaultSelection(),
                baseInterestRate.getStatus()
        );
    }


    public BaseInterestRate entityFromRequest(BaseInterestRateRequest request) {
        return BaseInterestRate.builder()
                .dateFrom(request.getDateFrom())
                .percentageRate(request.getPercentageRate())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus())
                .build();
    }

}
