package bg.energo.phoenix.service.nomenclature.document;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.nomenclature.document.DocumentExpirationPeriod;
import bg.energo.phoenix.model.request.nomenclature.document.ExpirationPeriodRequest;
import bg.energo.phoenix.model.response.nomenclature.document.ExpirationPeriodResponse;
import bg.energo.phoenix.repository.nomenclature.document.ExpirationPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpirationPeriodService {
    private final ExpirationPeriodRepository expirationPeriodRepository;

    @Transactional
    public ExpirationPeriodResponse edit(ExpirationPeriodRequest request) {
        DocumentExpirationPeriod expirationPeriod = expirationPeriodRepository.findActiveExpirationPeriod()
                .orElseThrow(() -> new DomainEntityNotFoundException("could not retrieve expiration period"));
        expirationPeriod.setNumberOfMonths(request.numberOfMonths());
        return new ExpirationPeriodResponse(expirationPeriod.getNumberOfMonths());
    }

    public ExpirationPeriodResponse view() {
        DocumentExpirationPeriod expirationPeriod = expirationPeriodRepository.findActiveExpirationPeriod()
                .orElseThrow(() -> new DomainEntityNotFoundException("could not retrieve expiration period"));
        return new ExpirationPeriodResponse(expirationPeriod.getNumberOfMonths());
    }
}
