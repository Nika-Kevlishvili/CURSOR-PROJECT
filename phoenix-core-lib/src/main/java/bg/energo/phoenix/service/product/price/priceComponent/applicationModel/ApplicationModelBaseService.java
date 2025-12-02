package bg.energo.phoenix.service.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.ApplicationModel;
import bg.energo.phoenix.model.request.product.price.aplicationModel.ApplicationModelRequest;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.ApplicationModelResponse;
import org.springframework.transaction.annotation.Transactional;

public interface ApplicationModelBaseService {

    @Transactional
    void create(ApplicationModel model, ApplicationModelRequest modelRequest);

    @Transactional
    void update(ApplicationModel applicationModel, ApplicationModelRequest modelRequest);

    @Transactional
    void delete(Long modelId);

    ApplicationModelResponse view(ApplicationModel model);

    @Transactional
    void clone(ApplicationModel source, ApplicationModel clone);

    @Transactional
    default boolean copy(ApplicationModel source, ApplicationModel copied, PriceComponent priceComponent) {
        throw new ClientException("Method not implemented!",ErrorCode.UNSUPPORTED_OPERATION);
    }

}
