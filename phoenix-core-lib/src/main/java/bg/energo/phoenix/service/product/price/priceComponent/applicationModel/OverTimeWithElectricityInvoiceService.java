package bg.energo.phoenix.service.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.ApplicationModel;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.OverTimeWithElectricityInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeWithElectricityType;
import bg.energo.phoenix.model.request.product.price.aplicationModel.ApplicationModelRequest;
import bg.energo.phoenix.model.request.product.price.aplicationModel.OverTimeWithElectricityInvoiceRequest;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.ApplicationModelResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.OverTimeWithElectricityInvoiceResponse;
import bg.energo.phoenix.repository.product.price.applicationModel.ApplicationModelRepository;
import bg.energo.phoenix.repository.product.price.applicationModel.OverTimeWithElectricityInvoiceRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import io.ebean.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
@Slf4j
public class OverTimeWithElectricityInvoiceService implements ApplicationModelBaseService {

    private final OverTimeWithElectricityInvoiceRepository overTimeWithElectricityInvoiceRepository;
    private final ApplicationModelRepository applicationModelRepository;
    private final PriceComponentRepository priceComponentRepository;

    private static OverTimeWithElectricityInvoice cloneOverTimeWithElectricityInvoice(ApplicationModel copied, OverTimeWithElectricityInvoice overTimeWithElectricityInvoice) {
        OverTimeWithElectricityInvoice clonedEntity = new OverTimeWithElectricityInvoice();
        clonedEntity.setType(overTimeWithElectricityInvoice.getType());
        clonedEntity.setPeriodType(overTimeWithElectricityInvoice.getPeriodType());
        clonedEntity.setApplicationModelId(copied.getId());
        clonedEntity.setStatus(EntityStatus.ACTIVE);
        return clonedEntity;
    }

    @Override
    @Transactional
    public void create(ApplicationModel model, ApplicationModelRequest request) {
        OverTimeWithElectricityInvoiceRequest overTimeWithElectricityInvoiceRequest = request.getOverTimeWithElectricityInvoiceRequest();
        OverTimeWithElectricityInvoice overTimeWithElectricityInvoice = new OverTimeWithElectricityInvoice(model, overTimeWithElectricityInvoiceRequest);
        overTimeWithElectricityInvoiceRepository.save(overTimeWithElectricityInvoice);
    }

    @Override
    @Transactional
    public void update(ApplicationModel applicationModel, ApplicationModelRequest request) {
        OverTimeWithElectricityInvoice overTimeWithElectricityInvoice = overTimeWithElectricityInvoiceRepository
                .findByApplicationModelIdAndStatusIn(applicationModel.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("priceComponentId-There is no OverTime WithElectricityInvoice Application model"));
        OverTimeWithElectricityInvoiceRequest overTimeWithElectricityInvoiceRequest = request.getOverTimeWithElectricityInvoiceRequest();
        overTimeWithElectricityInvoice.setType(Objects.requireNonNullElse(overTimeWithElectricityInvoiceRequest.getWithEveryInvoice(), false) ? OverTimeWithElectricityType.WITH_EVERY_INVOICE : OverTimeWithElectricityType.AT_MOST_ONCE);
        overTimeWithElectricityInvoice.setPeriodType(Objects.requireNonNullElse(overTimeWithElectricityInvoiceRequest.getAtMostOncePer(), false) ? overTimeWithElectricityInvoiceRequest.getOverTimeWithElectricityPeriodType() : null);
        overTimeWithElectricityInvoiceRepository.save(overTimeWithElectricityInvoice);
    }

    @Override
    @Transactional
    public void delete(Long modelId) {
        OverTimeWithElectricityInvoice overTimeWithElectricityInvoice = overTimeWithElectricityInvoiceRepository
                .findByApplicationModelIdAndStatusIn(modelId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("priceComponentId-There is no OverTime WithElectricityInvoice Application model"));
        overTimeWithElectricityInvoice.setStatus(EntityStatus.DELETED);
        overTimeWithElectricityInvoiceRepository.save(overTimeWithElectricityInvoice);
    }

    @Override
    public ApplicationModelResponse view(ApplicationModel model) {
        OverTimeWithElectricityInvoice overTimeWithElectricityInvoice = overTimeWithElectricityInvoiceRepository
                .findByApplicationModelIdAndStatusIn(model.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("priceComponentId-There is no OverTime WithElectricityInvoice Application model"));
        ApplicationModelResponse applicationModelResponse = new ApplicationModelResponse(model.getApplicationModelType(), model.getApplicationType(), model.getApplicationLevel());
        applicationModelResponse.setOverTimeWithElectricityInvoiceResponse(new OverTimeWithElectricityInvoiceResponse(overTimeWithElectricityInvoice));
        return applicationModelResponse;
    }

    @Override
    @Transactional
    public void clone(ApplicationModel source, ApplicationModel clone) {
        OverTimeWithElectricityInvoice overTimeWithElectricityInvoice = overTimeWithElectricityInvoiceRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-OverTime WithElectricityInvoice Application model not found while cloning application model ID %s;".formatted(source.getId())));

        OverTimeWithElectricityInvoice clonedEntity = cloneOverTimeWithElectricityInvoice(clone, overTimeWithElectricityInvoice);
        overTimeWithElectricityInvoiceRepository.save(clonedEntity);
    }

    @Override
    @Transactional
    public boolean copy(ApplicationModel source, ApplicationModel copied, PriceComponent priceComponent) {
        priceComponentRepository.saveAndFlush(priceComponent);
        copied.setPriceComponent(priceComponent);
        applicationModelRepository.saveAndFlush(copied);

        OverTimeWithElectricityInvoice overTimeWithElectricityInvoice = overTimeWithElectricityInvoiceRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-OverTime WithElectricityInvoice Application model not found while copying application model ID %s;".formatted(source.getId())));

        OverTimeWithElectricityInvoice clonedEntity = cloneOverTimeWithElectricityInvoice(copied, overTimeWithElectricityInvoice);
        overTimeWithElectricityInvoiceRepository.save(clonedEntity);
        return true;
    }
}
