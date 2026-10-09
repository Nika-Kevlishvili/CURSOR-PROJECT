package bg.energo.phoenix.bulgariapost.liabilities.service;

import bg.energo.phoenix.model.CheckLiabilityDetails;

import java.time.LocalDate;
import java.util.List;

public interface BulgariaPostOnlinePaymentCheckClient {
    List<CheckLiabilityDetails> check(
            Long collectionChannelId,
            Long customerId,
            String billingGroupNumber,
            LocalDate calculateDate
    );
}

