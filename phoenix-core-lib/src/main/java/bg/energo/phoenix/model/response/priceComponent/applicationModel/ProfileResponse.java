package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.nomenclature.pod.Profiles;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriodsProfiles;
import bg.energo.phoenix.model.response.nomenclature.pod.ProfilesResponse;
import lombok.Data;

@Data
public class ProfileResponse {
    private ProfilesResponse profile;
    private Double percentage;

    public ProfileResponse(SettlementPeriodsProfiles periodsProfiles, Profiles profiles) {
        this.profile = new ProfilesResponse(profiles);
        this.percentage = periodsProfiles.getPercentage();
    }
}
