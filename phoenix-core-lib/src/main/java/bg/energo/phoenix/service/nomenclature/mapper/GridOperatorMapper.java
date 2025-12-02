package bg.energo.phoenix.service.nomenclature.mapper;

import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.request.nomenclature.product.GridOperatorRequest;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.GridOperatorResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class GridOperatorMapper {

    public NomenclatureResponse nomenclatureResponseFromEntity(GridOperator gridOperator) {
        return new NomenclatureResponse(
                gridOperator.getId(),
                String.join(" - ", gridOperator.getName(), gridOperator.getFullName()),
                gridOperator.getOrderingId(),
                gridOperator.isDefaultSelection(),
                gridOperator.getStatus()
        );
    }

    public GridOperatorResponse responseFromEntity(GridOperator gridOperator) {
        return GridOperatorResponse.builder()
                .id(gridOperator.getId())
                .name(gridOperator.getName())
                .fullName(gridOperator.getFullName())
                .powerSupplyTerminationRequestEmail(gridOperator.getPowerSupplyTerminationRequestEmail())
                .powerSupplyReconnectionRequestEmail(gridOperator.getPowerSupplyReconnectionRequestEmail())
                .objectionToChangeCBGEmail(gridOperator.getObjectionToChangeCBGEmail())
                .codeForXEnergy(gridOperator.getCodeForXEnergy())
                .gridOperatorCode(gridOperator.getGridOperatorCode())
                .orderingId(gridOperator.getOrderingId())
                .defaultSelection(gridOperator.isDefaultSelection())
                .ownedByEnergoPro(gridOperator.getOwnedByEnergoPro())
                .status(gridOperator.getStatus())
                .build();
    }

    public GridOperator entityFromRequest(GridOperatorRequest request) {
        String terminationEmail = request.getPowerSupplyTerminationRequestEmail();
        String reconnectionEmail = request.getPowerSupplyReconnectionRequestEmail();
        String changeCBGEmail = request.getObjectionToChangeCBGEmail();
        Boolean isOwnedByEnergoPro = Objects.requireNonNullElse(request.getOwnedByEnergoPro(), false);

        return GridOperator.builder()
                .name(request.getName().trim())
                .fullName(request.getFullName().trim())
                .powerSupplyTerminationRequestEmail(StringUtils.isEmpty(terminationEmail) ? null : terminationEmail.trim())
                .powerSupplyReconnectionRequestEmail(StringUtils.isEmpty(reconnectionEmail) ? null : reconnectionEmail.trim())
                .objectionToChangeCBGEmail(StringUtils.isEmpty(changeCBGEmail) ? null : changeCBGEmail.trim())
                .codeForXEnergy(Objects.isNull(request.getCodeForXEnergy()) ? request.getCodeForXEnergy() : request.getCodeForXEnergy().replaceFirst("^0+", ""))
                .gridOperatorCode(Objects.isNull(request.getGridOperatorCode()) ? request.getGridOperatorCode() : request.getGridOperatorCode().replaceFirst("^0+", ""))
                .defaultSelection(request.getDefaultSelection())
                .ownedByEnergoPro(isOwnedByEnergoPro)
                .status(request.getStatus())
                .build();
    }

}
