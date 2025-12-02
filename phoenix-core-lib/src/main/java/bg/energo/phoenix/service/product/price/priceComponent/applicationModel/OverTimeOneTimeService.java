package bg.energo.phoenix.service.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.ApplicationModel;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.OverTimeOneTime;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeOneTimeStatus;
import bg.energo.phoenix.model.request.product.price.aplicationModel.ApplicationModelRequest;
import bg.energo.phoenix.model.request.product.price.aplicationModel.OverTimeOneTimeRequest;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.ApplicationModelResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.OverTimeOneTimeResponse;
import bg.energo.phoenix.repository.product.price.applicationModel.ApplicationModelRepository;
import bg.energo.phoenix.repository.product.price.applicationModel.OverTimeOneTimeRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class OverTimeOneTimeService implements ApplicationModelBaseService {

    private final OverTimeOneTimeRepository overTimeOneTimeRepository;
    private final ApplicationModelRepository applicationModelRepository;
    private final PriceComponentRepository priceComponentRepository;

    private static OverTimeOneTime cloneOverTimeOneTime(ApplicationModel copied, OverTimeOneTime overTimeOneTime) {
        OverTimeOneTime clonedEntity = new OverTimeOneTime();
        clonedEntity.setType(overTimeOneTime.getType());
        clonedEntity.setApplicationModel(copied);
        clonedEntity.setStatus(OverTimeOneTimeStatus.ACTIVE);
        return clonedEntity;
    }

    @Override
    public void create(ApplicationModel model, ApplicationModelRequest request) {
        OverTimeOneTimeRequest overTimeOneTimeRequest = request.getOverTimeOneTimeRequest();
        OverTimeOneTime overTimeOneTime = new OverTimeOneTime(model, overTimeOneTimeRequest);
        overTimeOneTimeRepository.save(overTimeOneTime);
    }

    @Override
    public void update(ApplicationModel applicationModel, ApplicationModelRequest request) {
        OverTimeOneTime overTimeOneTime = overTimeOneTimeRepository
                .findByApplicationModelIdAndStatusIn(applicationModel.getId(), List.of(OverTimeOneTimeStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("priceComponentId-There is no OverTime OneTime Application model", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        OverTimeOneTimeRequest overTimeOneTimeRequest = request.getOverTimeOneTimeRequest();
        overTimeOneTime.setType(overTimeOneTimeRequest.getType());
        overTimeOneTimeRepository.save(overTimeOneTime);
    }

    @Override
    public void delete(Long modelId) {
        OverTimeOneTime overTimeOneTime = overTimeOneTimeRepository
                .findByApplicationModelIdAndStatusIn(modelId, List.of(OverTimeOneTimeStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("priceComponentId-There is no OverTime OneTime Application model", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        overTimeOneTime.setStatus(OverTimeOneTimeStatus.DELETED);
        overTimeOneTimeRepository.save(overTimeOneTime);
    }

    @Override
    public ApplicationModelResponse view(ApplicationModel model) {
        OverTimeOneTime overTimeOneTime = overTimeOneTimeRepository
                .findByApplicationModelIdAndStatusIn(model.getId(), List.of(OverTimeOneTimeStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("priceComponentId-There is no OverTime OneTime Application model", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        ApplicationModelResponse applicationModelResponse = new ApplicationModelResponse(model.getApplicationModelType(), model.getApplicationType(), model.getApplicationLevel());
        applicationModelResponse.setOverTimeOneTimeResponse(new OverTimeOneTimeResponse(overTimeOneTime));
        return applicationModelResponse;
    }

    @Override
    public void clone(ApplicationModel source, ApplicationModel clone) {
        OverTimeOneTime overTimeOneTime = overTimeOneTimeRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(OverTimeOneTimeStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-OverTime OneTime Application model not found while cloning application model ID %s;".formatted(source.getId())));

        OverTimeOneTime clonedEntity = cloneOverTimeOneTime(clone, overTimeOneTime);
        overTimeOneTimeRepository.save(clonedEntity);
    }

    @Override
    public boolean copy(ApplicationModel source, ApplicationModel copied, PriceComponent priceComponent) {
        priceComponentRepository.saveAndFlush(priceComponent);
        copied.setPriceComponent(priceComponent);
        applicationModelRepository.saveAndFlush(copied);

        OverTimeOneTime overTimeOneTime = overTimeOneTimeRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(OverTimeOneTimeStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-OverTime OneTime Application model not found while copying application model ID %s;".formatted(source.getId())));

        OverTimeOneTime clonedEntity = cloneOverTimeOneTime(copied, overTimeOneTime);
        overTimeOneTimeRepository.save(clonedEntity);
        return true;
    }
}
