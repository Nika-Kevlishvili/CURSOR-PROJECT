package bg.energo.phoenix.service.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.ApplicationModel;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;
import bg.energo.phoenix.model.request.product.price.aplicationModel.ApplicationModelRequest;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.ApplicationModelResponse;
import bg.energo.phoenix.repository.product.price.applicationModel.ApplicationModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationModelService {

    private final ApplicationModelServiceFactory serviceFactory;
    private final ApplicationModelRepository applicationModelRepository;

    @Transactional
    public void create(PriceComponent priceComponent, ApplicationModelRequest request) {
        ApplicationModel model = new ApplicationModel();
        model.setApplicationModelType(request.getApplicationModelType());
        model.setApplicationType(request.getApplicationType());
        model.setApplicationLevel(request.getApplicationLevel());
        model.setStatus(ApplicationModelStatus.ACTIVE);
        model.setPriceComponent(priceComponent);
        ApplicationModel savedModel = applicationModelRepository.save(model);
        ApplicationModelBaseService modelService = serviceFactory.getModelService(request.getApplicationModelType(), request.getApplicationType());
        modelService.create(model, request);
    }


    @Transactional
    public void update(Long priceComponentId, ApplicationModelRequest request) {
        ApplicationModel applicationModel = applicationModelRepository
                .findByPriceComponentIdAndStatusIn(priceComponentId, List.of(ApplicationModelStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-ApplicationModel with id not found"));

        ApplicationModelType applicationModelType = applicationModel.getApplicationModelType();
        ApplicationType applicationType = applicationModel.getApplicationType();

        ApplicationModelType requestModelType = request.getApplicationModelType();
        ApplicationType requestApplicationType = request.getApplicationType();
        boolean isNotChanged = applicationModelType.equals(requestModelType) && Objects.equals(applicationType, requestApplicationType);
        ApplicationModelBaseService modelService = serviceFactory.getModelService(request.getApplicationModelType(), request.getApplicationType());

        if (isNotChanged) {
            modelService.update(applicationModel, request);
        } else {
            ApplicationModelBaseService oldModelService = serviceFactory.getModelService(applicationModel.getApplicationModelType(), applicationModel.getApplicationType());
            oldModelService.delete(applicationModel.getId());
            modelService.create(applicationModel, request);
        }
        applicationModel.setApplicationModelType(requestModelType);
        applicationModel.setApplicationType(request.getApplicationType());
        applicationModel.setApplicationLevel(request.getApplicationLevel());
        applicationModelRepository.save(applicationModel);
    }

    public ApplicationModelResponse view(Long priceComponentId) {
        ApplicationModel applicationModel = applicationModelRepository
                .findByPriceComponentIdAndStatusIn(priceComponentId, List.of(ApplicationModelStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Application model not found!"));
        ApplicationModelBaseService modelService = serviceFactory.getModelService(applicationModel.getApplicationModelType(), applicationModel.getApplicationType());
        ApplicationModelResponse view = modelService.view(applicationModel);
        view.setApplicationModelId(applicationModel.getId());
        return view;
    }

    @Transactional
    public void delete(Long priceComponentId) {
        ApplicationModel applicationModel = applicationModelRepository
                .findByPriceComponentIdAndStatusIn(priceComponentId, List.of(ApplicationModelStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Application model not found!"));

        ApplicationModelBaseService modelService = serviceFactory.getModelService(applicationModel.getApplicationModelType(), applicationModel.getApplicationType());
        modelService.delete(applicationModel.getId());
        applicationModel.setStatus(ApplicationModelStatus.DELETED);
        applicationModelRepository.save(applicationModel);
    }

    @Transactional
    public void clone(ApplicationModel source, PriceComponent clonedPriceComponent) {
        // clone main model
        ApplicationModel clonedApplicationModel = new ApplicationModel();
        clonedApplicationModel.setApplicationModelType(source.getApplicationModelType());
        clonedApplicationModel.setApplicationType(source.getApplicationType());
        clonedApplicationModel.setApplicationLevel(source.getApplicationLevel());
        clonedApplicationModel.setStatus(ApplicationModelStatus.ACTIVE);
        clonedApplicationModel.setPriceComponent(clonedPriceComponent);
        applicationModelRepository.saveAndFlush(clonedApplicationModel);

        // clone sub entities
        ApplicationModelBaseService modelService = serviceFactory.getModelService(source.getApplicationModelType(), source.getApplicationType());
        modelService.clone(source, clonedApplicationModel);
    }


    @Transactional
    public boolean copy(ApplicationModel source, ApplicationModel copied,PriceComponent priceComponent) {
        ApplicationModelBaseService modelService = serviceFactory.getModelService(source.getApplicationModelType(), source.getApplicationType());
        return modelService.copy(source, copied,priceComponent);
    }
}
