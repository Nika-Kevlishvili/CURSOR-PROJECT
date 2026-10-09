package bg.energo.phoenix.systech.liabilities.service;

import bg.energo.phoenix.systech.liabilities.api.SystechLiabilitiesResponse;
import bg.energo.phoenix.systech.liabilities.api.SystechLiabilityResultItem;
import bg.energo.phoenix.systech.liabilities.repository.SystechLiabilitiesRepository;
import bg.energo.phoenix.systech.liabilities.repository.SystechObligationsProjection;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SystechLiabilitiesService {

    private static final String COLLECTION_CHANNEL_NAME = "Systech";

    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");

    private static final String MSG_INVALID_CUSTOMER_NUMBER = "Невалиден клиентски номер.";
    private static final String MSG_CUSTOMER_NOT_FOUND = "Клиентският номер не съществува.";
    private static final String MSG_SYSTEM_ERROR = "Системна грешка";

    private final CollectionChannelRepository collectionChannelRepository;
    private final SystechLiabilitiesRepository liabilitiesRepository;
    private final CustomerRepository customerRepository;

    public SystechLiabilitiesService(
            CollectionChannelRepository collectionChannelRepository,
            SystechLiabilitiesRepository liabilitiesRepository,
            CustomerRepository customerRepository
    ) {
        this.collectionChannelRepository = Objects.requireNonNull(collectionChannelRepository, "collectionChannelRepository");
        this.liabilitiesRepository = Objects.requireNonNull(liabilitiesRepository, "liabilitiesRepository");
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository");
    }

    public SystechLiabilitiesResponse obligations(String customerNumberRaw) {
        try {
            final String customerNumber = trimToNull(customerNumberRaw);
            if (isInvalidCustomerNumber(customerNumber)) {
                return SystechLiabilitiesResponse.error(2, MSG_INVALID_CUSTOMER_NUMBER);
            }

            final Optional<CollectionChannel> channelOptional = collectionChannelRepository.findCollectionChanel(COLLECTION_CHANNEL_NAME);
            if (channelOptional.isEmpty()) {
                return SystechLiabilitiesResponse.error(4, MSG_SYSTEM_ERROR);
            }
            final CollectionChannel channel = channelOptional.get();

            if (!liabilitiesRepository.existsValidCustomerNumber(customerNumber)) {
                return SystechLiabilitiesResponse.error(3, MSG_CUSTOMER_NOT_FOUND);
            }

            ParsedCustomerNumber parsed = parseCustomerNumber(customerNumber);
            Long customerId = resolveCustomerId(parsed.customerNumber10());
            if (customerId == null) {
                return SystechLiabilitiesResponse.error(3, MSG_CUSTOMER_NOT_FOUND);
            }

            List<SystechObligationsProjection> rows = liabilitiesRepository.findObligations(
                    customerNumber,
                    channel.getId()
            );

            if (rows.isEmpty()) {
                return SystechLiabilitiesResponse.success(List.of());
            }

            final List<SystechLiabilityResultItem> results = rows.stream()
                    .map(SystechLiabilityResultItem::new)
                    .toList();

            return SystechLiabilitiesResponse.success(results);
        } catch (Exception e) {
            log.error("SystechLiabilitiesService.obligations", e);
            return SystechLiabilitiesResponse.error(4, MSG_SYSTEM_ERROR);
        }
    }

    private Long resolveCustomerId(String customerNumber10) {
        if (customerNumber10 == null) {
            return null;
        }
        return customerRepository
                .findByCustomerNumberAndStatus(Long.valueOf(customerNumber10), CustomerStatus.ACTIVE)
                .map(Customer::getId)
                .orElse(null);
    }

    private static ParsedCustomerNumber parseCustomerNumber(String customerNumber) {
        if (customerNumber.length() == 10) {
            return new ParsedCustomerNumber(customerNumber, null);
        }
        return new ParsedCustomerNumber(customerNumber.substring(0, 10), customerNumber.substring(10));
    }

    private record ParsedCustomerNumber(String customerNumber10, String billingGroup4) {
    }

    private static boolean isInvalidCustomerNumber(String value) {
        if (value == null) {
            return true;
        }
        if (value.length() > 20) {
            return true;
        }
        return !DIGITS_ONLY.matcher(value).matches();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

