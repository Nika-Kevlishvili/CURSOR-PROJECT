package bg.energo.phoenix.model.response.pod.billingByProfile;

import bg.energo.phoenix.model.entity.nomenclature.pod.Profiles;
import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingByProfile;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BillingByProfilePreviewResponse {

    private Long id;
    private Long podId;
    private String podIdentifier;
    private PeriodType periodType;
    private TimeZone timeZone;
    private Long profileId;
    private String profileName;
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
    private BillingByProfileStatus status;
    private List<BillingByProfileDataResponse> entries;
    private Boolean isLocked;

    public BillingByProfilePreviewResponse(BillingByProfile billingByProfile,
                                           PointOfDelivery pod,
                                           Profiles profile) {
        this.id = billingByProfile.getId();
        this.podId = pod.getId();
        this.podIdentifier = pod.getIdentifier();
        this.periodType = billingByProfile.getPeriodType();
        this.timeZone = billingByProfile.getTimeZone();
        this.profileId = profile.getId();
        this.profileName = profile.getName();
        this.periodFrom = billingByProfile.getPeriodFrom();
        this.periodTo = billingByProfile.getPeriodTo();
        this.status = billingByProfile.getStatus();
        this.isLocked = billingByProfile.getInvoiced() != null && billingByProfile.getInvoiced();
    }
}
