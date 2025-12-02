package bg.energo.phoenix.service.pod.billingByProfile;

import bg.energo.phoenix.model.entity.nomenclature.pod.Profiles;
import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingByProfile;
import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingDataByProfile;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.request.pod.billingByProfile.BillingByProfileCreateRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillingByProfileMapper {

    public BillingByProfile fromRequestToBillingByProfileEntity(BillingByProfileCreateRequest request, Profiles profiles, PointOfDelivery pod) {
        BillingByProfile billingByProfile = new BillingByProfile();
        billingByProfile.setPodId(pod.getId());
        billingByProfile.setProfileId(request.getProfileId());
        billingByProfile.setPeriodType(request.getPeriodType());
        if (List.of(PeriodType.FIFTEEN_MINUTES, PeriodType.ONE_HOUR).contains(request.getPeriodType())) {
            billingByProfile.setTimeZone(profiles.getTimeZone());
        }
        billingByProfile.setPeriodFrom(request.getPeriodFrom());
        billingByProfile.setPeriodTo(request.getPeriodTo());
        billingByProfile.setStatus(BillingByProfileStatus.ACTIVE);
        return billingByProfile;
    }


    public BillingDataByProfile fromRequestToBillingDataByProfileEntity(LocalDateTime periodFrom,
                                                                        LocalDateTime periodTo,
                                                                        BigDecimal value,
                                                                        Long billingByProfileId,
                                                                        Boolean isShiftedHour) {
        BillingDataByProfile data = new BillingDataByProfile();
        data.setBillingByProfileId(billingByProfileId);
        data.setPeriodFrom(periodFrom);
        data.setPeriodTo(periodTo);
        data.setValue(value);
        data.setIsShiftedHour(isShiftedHour);
        return data;
    }

}
