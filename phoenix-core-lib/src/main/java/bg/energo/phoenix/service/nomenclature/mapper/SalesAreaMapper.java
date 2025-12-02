package bg.energo.phoenix.service.nomenclature.mapper;

import bg.energo.phoenix.model.entity.nomenclature.product.SalesArea;
import bg.energo.phoenix.model.request.nomenclature.product.SalesAreaRequest;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesAreaResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class SalesAreaMapper {

    public NomenclatureResponse nomenclatureResponseFromEntity(SalesArea salesArea) {
        String name = salesArea.getName();
        if (StringUtils.isNotBlank(salesArea.getLoginPortalTag())) {
            name = String.join(" - ", name, salesArea.getLoginPortalTag());
        }

        return new NomenclatureResponse(
                salesArea.getId(),
                name,
                salesArea.getOrderingId(),
                salesArea.isDefaultSelection(),
                salesArea.getStatus()
        );
    }

    public SalesAreaResponse responseFromEntity(SalesArea salesArea) {
        return SalesAreaResponse.builder()
                .id(salesArea.getId())
                .name(salesArea.getName())
                .loginPortalTag(salesArea.getLoginPortalTag())
                .orderingId(salesArea.getOrderingId())
                .defaultSelection(salesArea.isDefaultSelection())
                .status(salesArea.getStatus())
                .build();
    }

    public SalesArea entityFromRequest(SalesAreaRequest request) {
        return SalesArea.builder()
                .name(request.getName().trim())
                .loginPortalTag(StringUtils.isEmpty(request.getLoginPortalTag()) ? null : request.getLoginPortalTag().trim())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus()).build();
    }
}
