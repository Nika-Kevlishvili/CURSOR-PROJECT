package bg.energo.phoenix.service.xEnergie.jobs.service;

import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.service.contract.product.dealCreationEvent.ProductContractDealCreationEvent;
import bg.energo.phoenix.service.xEnergie.jobs.enums.XEnergieJobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Profile({"dev","test"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class XEnergieDealCreationService extends AbstractXEnergieService {
    private final XEnergieSplitUpdaterService xEnergieSplitUpdaterService;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ApplicationEventPublisher eventPublisher;

    public XEnergieDealCreationService(XEnergieSchedulerErrorHandler xEnergieSchedulerErrorHandler,
                                       XEnergieSplitUpdaterService xEnergieSplitUpdaterService,
                                       ProductContractDetailsRepository productContractDetailsRepository,
                                       ApplicationEventPublisher eventPublisher) {
        super(xEnergieSchedulerErrorHandler);
        this.xEnergieSplitUpdaterService = xEnergieSplitUpdaterService;
        this.productContractDetailsRepository = productContractDetailsRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected XEnergieJobType getJobType() {
        return XEnergieJobType.X_ENERGIE_DEAL_CREATION;
    }

    @Override
    protected AbstractXEnergieService getNextJobInChain() {
        return xEnergieSplitUpdaterService;
    }

    @Override
    public void execute(Process process) {
        try {
            log.debug("Calculating date ranges");
            LocalDateTime yesterdayStart = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime yesterdayEnd = yesterdayStart.plusDays(1).minusSeconds(1);
            log.debug("Start date: [%s], end date: [%s]".formatted(yesterdayStart, yesterdayEnd));

            long totalProductContractsThatShouldBeUpdated = productContractDetailsRepository
                    .countProductContractDetailsForDealCreation(yesterdayStart, yesterdayEnd);
            log.debug("Total Product Contract Details count without deal that should be updated: [%s]".formatted(totalProductContractsThatShouldBeUpdated));
            long updatedProductContractCount = 0;
            int offset = 0;
            Integer batchSize = getProperties().batchSize();

            while (totalProductContractsThatShouldBeUpdated > updatedProductContractCount) {
                log.debug("Fetching batch: offset: [%s], size: [%s]".formatted(offset, batchSize));
                List<ProductContractDetails> productContractDetailsForDealCreation = productContractDetailsRepository
                        .findProductContractDetailsForDealCreation(yesterdayStart, yesterdayEnd, PageRequest.of(offset, batchSize));
                log.debug("Fetched product contract details count: [%s]".formatted(productContractDetailsForDealCreation.size()));

                log.debug("Publishing event to create deal");
                productContractDetailsForDealCreation
                        .stream()
                        .map(ProductContractDealCreationEvent::new)
                        .forEach(event -> {
                            log.debug("Publishing event for product contract details with id: [%s]".formatted(event.getProductContractDetails().getId()));
                            eventPublisher.publishEvent(event);
                        });

                offset++;
                updatedProductContractCount += batchSize;
            }
        } catch (Exception e) {
            log.debug("Exception handled: ", e);
        } finally {
            executeNextJobInChain(process);
        }
    }

    public List<ProductContractDetails> getProductContractDetailsWithoutDeal() {
        return productContractDetailsRepository
                .findProductContractDetailsForDealCreation(
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.of(2030, 1, 1, 0, 0),
                        PageRequest.of(0, Integer.MAX_VALUE)
                );
    }
}
