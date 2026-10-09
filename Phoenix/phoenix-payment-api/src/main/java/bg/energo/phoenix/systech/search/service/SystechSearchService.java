package bg.energo.phoenix.systech.search.service;

import bg.energo.phoenix.systech.search.api.SystechSearchRequest;
import bg.energo.phoenix.systech.search.api.SystechSearchResponse;
import bg.energo.phoenix.systech.search.api.SystechSearchResultItem;
import bg.energo.phoenix.systech.search.repository.SystechSearchRepository;
import bg.energo.phoenix.systech.search.repository.CustomerSearchProjection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SystechSearchService {

    private static final int MIN_PARTIAL_MATCH = 3;

    private static final int MAX_CUSTOMER_NUMBER_LEN = 20;
    private static final int MAX_CUSTOMER_NAME_LEN = 512;
    private static final int MAX_CUSTOMER_ADDRESS_LEN = 2048;
    private static final int MAX_CUSTOMER_PIN_LEN = 20;
    private static final int MAX_CUSTOMER_PHONE_LEN = 20;
    private static final int MAX_CUSTOMER_POD_LEN = 64;

    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");
    private static final Pattern DIGITS_AND_PLUS = Pattern.compile("^[\\d+]+$");

    private static final String MSG_INVALID_CUSTOMER_NUMBER = "Невалиден клиентски номер";
    private static final String MSG_INVALID_CUSTOMER_NAME = "Невалидно име";
    private static final String MSG_INVALID_CUSTOMER_ADDRESS = "Невалиден адрес";
    private static final String MSG_INVALID_CUSTOMER_PIN = "Невалиден идентификатор";
    private static final String MSG_INVALID_CUSTOMER_PHONE = "Невалиден телефонен номер";
    private static final String MSG_INVALID_CUSTOMER_POD = "Невалидна точка на отчет";
    private static final String MSG_MORE_THAN_100_RECORD = "Намерени са повече от 100 клиента. Моля, конкретизирайте заявката";
    private static final String MSG_SYSTEM_ERROR = "Системна грешка";

    private final SystechSearchRepository searchRepository;

    public SystechSearchService(SystechSearchRepository searchRepository) {
        this.searchRepository = Objects.requireNonNull(searchRepository, "searchRepository");
    }

    public SystechSearchResponse search(SystechSearchRequest request) {
        try {
            if (request == null) {
                return SystechSearchResponse.error(9, MSG_SYSTEM_ERROR);
            }

            final String customerNumber = trimToNull(request.getCustomerNumber());
            final String customerName = trimToNull(request.getCustomerName());
            final String customerAddress = trimToNull(request.getCustomerAddress());
            final String customerPin = trimToNull(request.getCustomerPIN());
            final String customerPhone = trimToNull(request.getCustomerPhone());
            final String customerPod = trimToNull(request.getCustomerPOD());

            if (isInvalidCustomerNumber(customerNumber)) {
                return SystechSearchResponse.error(2, MSG_INVALID_CUSTOMER_NUMBER);
            }
            if (isInvalidPartial(customerName, MAX_CUSTOMER_NAME_LEN)) {
                return SystechSearchResponse.error(3, MSG_INVALID_CUSTOMER_NAME);
            }
            if (isInvalidPartial(customerAddress, MAX_CUSTOMER_ADDRESS_LEN)) {
                return SystechSearchResponse.error(4, MSG_INVALID_CUSTOMER_ADDRESS);
            }
            if (isInvalidPartial(customerPin, MAX_CUSTOMER_PIN_LEN)) {
                return SystechSearchResponse.error(5, MSG_INVALID_CUSTOMER_PIN);
            }
            if (isInvalidCustomerPhone(customerPhone)) {
                return SystechSearchResponse.error(6, MSG_INVALID_CUSTOMER_PHONE);
            }
            if (isInvalidPartial(customerPod, MAX_CUSTOMER_POD_LEN)) {
                return SystechSearchResponse.error(7, MSG_INVALID_CUSTOMER_POD);
            }

            List<CustomerSearchProjection> records = searchRepository.searchCustomers(
                    customerNumber,
                    customerName,
                    customerAddress,
                    customerPin,
                    customerPhone,
                    customerPod
            );

            if (records.size() > 100) {
                return SystechSearchResponse.error(8, MSG_MORE_THAN_100_RECORD);
            }

            List<SystechSearchResultItem> results = records.stream()
                    .map(this::toResultItem)
                    .collect(Collectors.toList());

            return SystechSearchResponse.success(results);
        } catch (Exception e) {
            return SystechSearchResponse.error(9, MSG_SYSTEM_ERROR);
        }
    }

    private SystechSearchResultItem toResultItem(CustomerSearchProjection record) {
        return new SystechSearchResultItem(
                record.getCustomerNumber(),
                record.getCustomerName(),
                record.getCustomerAddress(),
                record.getCustomerPIN(),
                record.getCustomerPhone()
        );
    }

    private static boolean isInvalidCustomerNumber(String value) {
        if (value == null) {
            return false;
        }
        return value.length() > MAX_CUSTOMER_NUMBER_LEN
                || value.length() < MIN_PARTIAL_MATCH
                || !DIGITS_ONLY.matcher(value).matches();
    }

    private static boolean isInvalidCustomerPhone(String value) {
        if (value == null) {
            return false;
        }
        if (value.length() > MAX_CUSTOMER_PHONE_LEN) {
            return true;
        }
        if (!DIGITS_AND_PLUS.matcher(value).matches()) {
            return true;
        }
        return value.length() < MIN_PARTIAL_MATCH;
    }

    private static boolean isInvalidPartial(String value, int maxLength) {
        if (value == null) {
            return false;
        }
        return value.length() > maxLength || value.length() < MIN_PARTIAL_MATCH;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
