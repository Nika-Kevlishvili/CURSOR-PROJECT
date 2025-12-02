package bg.energo.phoenix.model.request.pod.billingByProfile;

import bg.energo.phoenix.model.request.pod.billingByProfile.data.BillingByProfileDataUpdateRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BillingByProfileUpdateRequest {

    private List<@Valid BillingByProfileDataUpdateRequest> entries;

}
