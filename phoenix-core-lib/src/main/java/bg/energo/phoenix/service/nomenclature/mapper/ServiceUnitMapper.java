package bg.energo.phoenix.service.nomenclature.mapper;

import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceUnit;
import bg.energo.phoenix.model.request.nomenclature.product.service.ServiceUnitRequest;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.ServiceUnitResponse;
import org.springframework.stereotype.Service;

@Service
public class ServiceUnitMapper {

    public NomenclatureResponse nomenclatureResponseFromEntity(ServiceUnit serviceUnit) {
        return new NomenclatureResponse(
                serviceUnit.getId(),
                serviceUnit.getName(),
                serviceUnit.getOrderingId(),
                serviceUnit.isDefaultSelection(),
                serviceUnit.getStatus()
        );
    }

    public ServiceUnitResponse responseFromEntity(ServiceUnit serviceUnit) {
        return ServiceUnitResponse.builder()
                .id(serviceUnit.getId())
                .name(serviceUnit.getName())
                .orderingId(serviceUnit.getOrderingId())
                .defaultSelection(serviceUnit.isDefaultSelection())
                .status(serviceUnit.getStatus())
                .build();
    }

    public ServiceUnit entityFromRequest(ServiceUnitRequest request) {
        return ServiceUnit.builder()
                .name(request.getName().trim())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus()).build();
    }

}
