package bg.energo.phoenix.service.product.service;

import bg.energo.phoenix.model.entity.product.service.ServiceContractTerm;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.CreateServiceContractTermRequest;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.EditServiceContractTermRequest;
import bg.energo.phoenix.model.response.service.ContractTermNameResponse;
import bg.energo.phoenix.repository.product.service.subObject.ServiceContractTermRepository;
import bg.energo.phoenix.util.StringUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceContractTermService {

    private final ServiceMapper serviceMapper;
    private final ServiceContractTermRepository serviceContractTermRepository;

    @Transactional
    public void createServiceContractTerms(ServiceDetails serviceDetails, List<CreateServiceContractTermRequest> contractTerms, List<String> exceptionMessages) {
        List<ServiceContractTerm> serviceContractTerms = contractTerms
                .stream()
                .map(t -> serviceMapper.fromCreateRequestToServiceContractTermEntity(t, serviceDetails))
                .toList();
        serviceContractTermRepository.saveAll(serviceContractTerms);
    }

    /**
     * Get distinct contract terms by name and status
     *
     * @param statuses List of statuses to filter by
     * @return List of distinct contract terms
     */
    public Page<ContractTermNameResponse> getDistinctContractTermsByNameAndStatus(List<ServiceSubobjectStatus> statuses,
                                                                                  int page,
                                                                                  int size,
                                                                                  String prompt) {
        return serviceContractTermRepository.findDistinctNameByStatusIn(
                statuses,
                EPBStringUtils.fromPromptToQueryParameter(StringUtil.underscoreReplacer(prompt)),
                PageRequest.of(page, size)
        );
    }


    /**
     * Update service contract terms and delete the ones that are not present in the request
     *
     * @param requestContractTerms  List of contract terms to update
     * @param updatedServiceDetails Updated service details instance
     * @param exceptionMessages     List of exception messages
     */
    public void updateServiceContractTerms(List<EditServiceContractTermRequest> requestContractTerms,
                                           ServiceDetails updatedServiceDetails,
                                           List<String> exceptionMessages) {
        List<ServiceContractTerm> dbContractTerms = serviceContractTermRepository
                .findByServiceDetailsIdAndStatusIn(updatedServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        List<ServiceContractTerm> tempList = new ArrayList<>();

        for (int i = 0, contractTermsSize = requestContractTerms.size(); i < contractTermsSize; i++) {
            EditServiceContractTermRequest request = requestContractTerms.get(i);
            if (request.getId() == null) {
                // new contract term was added
                create(updatedServiceDetails, request, tempList);
            } else {
                // existing contract term was updated
                update(updatedServiceDetails, exceptionMessages, dbContractTerms, i, request, tempList);
            }
        }

        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            return;
        }

        // save updated contract terms
        serviceContractTermRepository.saveAll(tempList);

        List<Long> requests = requestContractTerms.stream()
                .map(EditServiceContractTermRequest::getId)
                .filter(Objects::nonNull).toList();

        // delete contract terms that are not present in the updated list
        for (ServiceContractTerm dbContractTerm : dbContractTerms) {
            if (!requests.contains(dbContractTerm.getId())) {
                dbContractTerm.setStatus(ServiceSubobjectStatus.DELETED);
                serviceContractTermRepository.save(dbContractTerm);
            }
        }
    }


    /**
     * Create new contract term and save it to the database
     *
     * @param updatedServiceDetails Updated service details instance
     * @param request               Request to create new contract term
     */
    private void create(ServiceDetails updatedServiceDetails,
                        EditServiceContractTermRequest request,
                        List<ServiceContractTerm> tempList) {
        ServiceContractTerm newContractTerm = serviceMapper.fromCreateRequestToServiceContractTermEntity(request, updatedServiceDetails);
        tempList.add(newContractTerm);
    }


    /**
     * Update existing contract term and save it to the database
     *
     * @param updatedServiceDetails Updated service details instance
     * @param exceptionMessages     List of exception messages
     * @param dbContractTerms       List of contract terms from the database
     * @param index                 Index of the contract term in the list
     * @param request               Request to update the contract term
     */
    private void update(ServiceDetails updatedServiceDetails,
                        List<String> exceptionMessages,
                        List<ServiceContractTerm> dbContractTerms,
                        int index,
                        EditServiceContractTermRequest request,
                        List<ServiceContractTerm> tempList) {
        Optional<ServiceContractTerm> contractTermOptional = dbContractTerms
                .stream()
                .filter(t -> t.getId().equals(request.getId()))
                .findFirst();

        if (contractTermOptional.isEmpty()) {
            log.error("Contract term with id {} not found", request.getId());
            exceptionMessages.add("contractTerms[%s].id-Contract term with id %s not found;".formatted(index, request.getId()));
            return;
        }

        ServiceContractTerm updatedContractTerm = serviceMapper
                .fromEditRequestToServiceContractTermEntity(
                        request,
                        contractTermOptional.get(),
                        updatedServiceDetails
                );

        tempList.add(updatedContractTerm);
    }
}
