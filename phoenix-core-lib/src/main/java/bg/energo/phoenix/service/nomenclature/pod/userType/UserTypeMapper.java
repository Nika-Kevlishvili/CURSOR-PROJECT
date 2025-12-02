package bg.energo.phoenix.service.nomenclature.pod.userType;

import bg.energo.phoenix.model.entity.nomenclature.pod.UserType;
import bg.energo.phoenix.model.request.nomenclature.pod.UserTypeRequest;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.pod.UserTypeResponse;
import org.springframework.stereotype.Service;

@Service
public class UserTypeMapper {

    public NomenclatureResponse nomenclatureResponseFromEntity(UserType userType) {
        return new NomenclatureResponse(
                userType.getId(),
                userType.getName(),
                userType.getOrderingId(),
                userType.isDefaultSelection(),
                userType.getStatus()
        );
    }

    public UserType entityFromRequest(UserTypeRequest request) {
        return UserType.builder()
                .name(request.getName().trim())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus())
                .build();
    }

    public UserTypeResponse responseFromEntity(UserType userType) {
        return UserTypeResponse.builder()
                .id(userType.getId())
                .name(userType.getName())
                .orderingId(userType.getOrderingId())
                .defaultSelection(userType.isDefaultSelection())
                .status(userType.getStatus())
                .build();
    }

}
