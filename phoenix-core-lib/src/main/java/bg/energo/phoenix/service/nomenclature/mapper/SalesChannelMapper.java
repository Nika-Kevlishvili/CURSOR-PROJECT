package bg.energo.phoenix.service.nomenclature.mapper;

import bg.energo.phoenix.model.entity.nomenclature.product.SalesChannel;
import bg.energo.phoenix.model.request.nomenclature.product.SalesChannelRequest;
import bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesChannelResponse;
import org.springframework.stereotype.Service;

@Service
public class SalesChannelMapper {



    public SalesChannelResponse responseFromEntity(SalesChannel salesChannel, PortalTagResponse portalTag) {
        return SalesChannelResponse.builder()
                .id(salesChannel.getId())
                .name(salesChannel.getName())
                .portalTagResponse(portalTag)
                .offPremisesContracts(salesChannel.getOffPremisesContracts())
                .orderingId(salesChannel.getOrderingId())
                .defaultSelection(salesChannel.isDefaultSelection())
                .status(salesChannel.getStatus())
                .build();
    }

    public SalesChannel entityFromRequest(SalesChannelRequest request) {
        return SalesChannel.builder()
                .name(request.getName().trim())
                .portalTagId(request.getPortalTagId())
                .offPremisesContracts(request.getOffPremisesContracts())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus()).build();
    }

}
