package bg.energo.phoenix.util.nomenclature;

import lombok.Getter;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Getter
public class NomenclatureProperties {

    private final Long measuredQuantityProfile;

    public NomenclatureProperties(Environment environment) {
        this.measuredQuantityProfile = environment.getProperty("billing.nomenclature.profile.measuredQuantity", Long.class);
    }
}
