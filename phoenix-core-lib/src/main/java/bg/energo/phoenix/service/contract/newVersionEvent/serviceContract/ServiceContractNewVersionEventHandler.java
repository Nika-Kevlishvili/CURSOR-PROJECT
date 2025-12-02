package bg.energo.phoenix.service.contract.newVersionEvent.serviceContract;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractEditRequest;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractVersions;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.service.contract.service.ServiceContractAdditionalParametersService;
import bg.energo.phoenix.service.contract.service.ServiceContractBasicParametersService;
import bg.energo.phoenix.service.contract.service.ServiceContractService;
import bg.energo.phoenix.service.contract.service.ServiceContractServiceParametersService;
import bg.energo.phoenix.service.customer.CustomerMapperService;
import bg.energo.phoenix.service.product.service.ServiceRelatedContractUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceContractNewVersionEventHandler {
    private final CustomerMapperService customerMapperService;
    private final ServiceContractBasicParametersService serviceContractBasicParametersService;
    private final ServiceContractAdditionalParametersService serviceContractAdditionalParametersService;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ServiceContractServiceParametersService serviceParametersService;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ServiceContractService serviceContractService;
    private final ServiceRelatedContractUpdateService serviceRelatedContractUpdateService;


    @Async
    @Transactional
    @EventListener(ServiceContractCreateNewVersionEvent.class)
    public void handleEvent(ServiceContractCreateNewVersionEvent event) {
        log.info("Start creating new service contract version");
        //create new version of service Contract and fill with new service options
        ServiceContractResponse serviceContractResponse = getServiceContractResponse(event.getServiceRelatedContractId(), event.getServiceRelatedContractVersion());
        ServiceContractEditRequest serviceContractEditRequest = customerMapperService.mapServiceContractEditRequest(serviceContractResponse, LocalDate.now(), event.getServiceRelatedContractCustomerDetailId());
        Long updatedContractId = serviceContractService.update(serviceContractEditRequest, event.getServiceRelatedContractId(), event.getServiceRelatedContractVersion(), true);
        Long maxVersionId = serviceContractDetailsRepository.findMaxVersionId(updatedContractId);

        Optional<ServiceContractDetails> updatedSerContrOptional = serviceContractDetailsRepository.findByContractIdAndVersionId(updatedContractId, maxVersionId);

        if (updatedSerContrOptional.isPresent()) {
            ServiceContractDetails updatedSerContr = updatedSerContrOptional.get();
            serviceRelatedContractUpdateService.updateSpecificServiceContractDetail(event.getCurrentServiceDetails(), updatedSerContr,
                    event.getProductContractValidTerm(), event.getExceptionMessagesContext());
        }
    }

    public ServiceContractResponse getServiceContractResponse(Long id, Long versionId) {
        log.debug("View service contract with id: {}", id);

        ServiceContracts serviceContracts = serviceContractsRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE, EntityStatus.DELETED)).
                orElseThrow(() -> new DomainEntityNotFoundException("id-Can't find Contract with id: %s".formatted(id)));

        ServiceContractDetails details;
        if (versionId != null) {
            details = serviceContractDetailsRepository
                    .findByContractIdAndVersionId(id, versionId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't Find Service ContractDetails with id: %s and version: %s;".formatted(id, versionId)));
        } else {
            details = serviceContractDetailsRepository
                    .findFirstByContractIdOrderByVersionIdDesc(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Service Contract details with contract id: %s;".formatted(id)));
        }

        ServiceContractResponse response = new ServiceContractResponse();

        response.setBasicParameters(serviceContractBasicParametersService.getBasicParameters(serviceContracts, details));
        response.setAdditionalParameters(serviceContractAdditionalParametersService.getAdditionalParameters(details));
        response.setThirdPageTabs(serviceParametersService.thirdPageFields(getServiceDetails(details.getServiceDetailId())));
        response.setServiceParameters(serviceParametersService.thirdPagePreview(serviceContracts, details, getServiceInfo(details), serviceContracts.getContractNumber()));
        response.setVersions(getContractVersions(id));
        response.setStatus(serviceContracts.getStatus());
        return response;
    }

    private ServiceDetails getServiceDetails(Long lastServiceDetailId) {
        if (lastServiceDetailId != null) {
            return serviceDetailsRepository.findById(lastServiceDetailId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("serviceContractBasicParametersCreateRequest.serviceId- [serviceId] can't find service details;"));

        } else
            throw new DomainEntityNotFoundException("serviceContractBasicParametersCreateRequest.serviceId- [serviceId] can't find service details;");
    }

    private ServiceDetails getServiceInfo(ServiceContractDetails details) {
        return serviceDetailsRepository.findById(details.getServiceDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Attached service do not exist;"));
    }

    private List<ServiceContractVersions> getContractVersions(Long id) {
        List<ServiceContractDetails> version = serviceContractDetailsRepository.findAllByContractIdOrderByStartDateDesc(id);
        List<ServiceContractVersions> returnList = new ArrayList<>();
        for (ServiceContractDetails item : version) {
            returnList.add(ServiceContractVersions.builder()
                    .id(item.getId())
                    .serviceId(item.getContractId())
                    .versionId(item.getVersionId())
                    .startDate(item.getStartDate())
                    .build());
        }
        return returnList;
    }

}
