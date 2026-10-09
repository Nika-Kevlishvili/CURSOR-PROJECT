package bg.energo.migration.customer.mapper.manager;

import bg.energo.migration.customer.entity.manager.CustomerManagerView;
import bg.energo.migration.customer.models.CustomerManagerViewMiddleResponse;
import bg.energo.migration.customer.models.CustomerManagerViewResponse;
import bg.energo.migration.integration.phoenix.customer.model.request.managers.CreateManagerRequest;
import bg.energo.migration.utils.ManagerUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerManagerMapper {
    public List<CreateManagerRequest> map(List<CustomerManagerViewResponse> customerManagerViewList) {
        return customerManagerViewList
                .stream()
                .map(this::mapToCreateManagerRequest)
                .toList();
    }

    private CreateManagerRequest mapToCreateManagerRequest(CustomerManagerViewResponse customerManagerView) {
        return CreateManagerRequest
                .builder()
                .titleId(customerManagerView.getTitleId())
                .name(customerManagerView.getName())
                .middleName(customerManagerView.getMiddleName())
                .surname(customerManagerView.getSurname())
                .personalNumber(ManagerUtil.validateManagerPersonalNumber(customerManagerView.getPersonalNumber()))
                .jobPosition(ManagerUtil.validateJobPosition(customerManagerView.getJobPosition()))
                .representationMethodId(customerManagerView.getRepresentationMethodId())
                .status(customerManagerView.getStatus())
                .build();
    }
}
