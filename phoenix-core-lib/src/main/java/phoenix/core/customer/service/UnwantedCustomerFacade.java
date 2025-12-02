package phoenix.core.customer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import phoenix.core.customer.apis.model.CustomerCheckResponse;
import phoenix.core.customer.apis.service.ApisService;
import phoenix.core.customer.model.entity.customer.UnwantedCustomer;
import phoenix.core.customer.model.entity.nomenclature.customer.UnwantedCustomerReason;
import phoenix.core.customer.model.enums.customer.unwantedCustomer.UnwantedCustomerSortField;
import phoenix.core.customer.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.unwantedCustomer.UnwantedCustomerCreateRequest;
import phoenix.core.customer.model.request.unwantedCustomer.UnwantedCustomerFilterRequest;
import phoenix.core.customer.model.response.customer.EditCustomerCreateRequest;
import phoenix.core.customer.model.response.customer.UnwantedCustomer.UnwantedCustomerResponse;
import phoenix.core.customer.repository.customer.UnwantedCustomerRepository;
import phoenix.core.customer.repository.nomenclature.customer.UnwantedCustomerReasonRepository;
import phoenix.core.exception.ClientException;
import phoenix.core.exception.ErrorCode;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service("coreUnwantedCustomerService")
@RequiredArgsConstructor
@Validated
public class UnwantedCustomerFacade {
    private final UnwantedCustomerRepository unwantedCustomerRepository;
    private final ApisService apisService;
    private final UnwantedCustomerReasonRepository unwantedCustomerReasonRepository;

    public UnwantedCustomerResponse createUnwantedCustomer(UnwantedCustomerCreateRequest request) {
        log.debug("Creating unwanted customer: {}", request.toString());
        String identificationNumber = request.getIdentificationNumber();
        UnwantedCustomer unwantedCustomer;
        Optional<UnwantedCustomerReason> unwantedCustomerReasonOptional =
                unwantedCustomerReasonRepository.findByIdAndStatusIn(request.getUnwantedCustomerReasonId(),
                        List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE));
        if(unwantedCustomerReasonOptional.isPresent()){
            UnwantedCustomerReason unwantedCustomerReason =unwantedCustomerReasonOptional.get();
            Optional<UnwantedCustomer> unwantedCustomerOptional =
                    unwantedCustomerRepository.findByIdentifier(identificationNumber);
            if (unwantedCustomerOptional.isPresent()) {
                throw new ClientException("This UIC/Personal number is already added", ErrorCode.APPLICATION_ERROR);
            }
            if (checkPrivateCustomer(identificationNumber)) {
                unwantedCustomer = saveUnwantedCustomer(request);
            } else {
                CustomerCheckResponse apisCustomer =
                        apisService.checkApisCustomerInfoWithSingleIdentificationNumber(identificationNumber);
                if (apisCustomer != null) {
                    unwantedCustomer = saveUnwantedCustomer(request);
                } else {
                    throw new ClientException("This UIC/Personal number is not valid", ErrorCode.APPLICATION_ERROR);
                }
            }
            return new UnwantedCustomerResponse(unwantedCustomer,unwantedCustomerReason.getName());
        } else {
            throw new ClientException("This Unwanted Customer Reason Id doesn't exists or is Deleted", ErrorCode.APPLICATION_ERROR);
        }
    }

    @Transactional
    public UnwantedCustomer saveUnwantedCustomer(UnwantedCustomerCreateRequest request) {
        UnwantedCustomer unwantedCustomer = new UnwantedCustomer();
        unwantedCustomer.setIdentifier(request.getIdentificationNumber());
        unwantedCustomer.setName(request.getName());
        unwantedCustomer.setUnwantedCustomerReasonId(request.getUnwantedCustomerReasonId());
        unwantedCustomer.setAdditionalInfo(request.getAdditionalInfo());
        unwantedCustomer.setCreateContractRestriction(request.getContractCreateRestriction());
        unwantedCustomer.setCreateOrderRestriction(request.getOrderCreateRestriction());
        unwantedCustomer.setSystemUserid("SysUser");//TODO sysUser add
        unwantedCustomer.setStatus(UnwantedCustomerStatus.ACTIVE);
        unwantedCustomer.setCreateDate(new Date());
        return unwantedCustomerRepository.save(unwantedCustomer);
    }

    private Boolean checkPrivateCustomer(String identificationNumber) {
        if (identificationNumber.length() == 10 || identificationNumber.length() == 12) {
            return true;
        } else return false;
    }

    @Transactional
    public UnwantedCustomerResponse edit(Long id, EditCustomerCreateRequest request) {
        log.debug("Editing unwanted customer: {}", request.toString());
        Optional<UnwantedCustomer> unwantedCustomerOptional = unwantedCustomerRepository.findById(id);
        if (unwantedCustomerOptional.isPresent()) {
            UnwantedCustomer unwantedCustomer = unwantedCustomerOptional.get();
            if (unwantedCustomer.getStatus().equals(UnwantedCustomerStatus.DELETED)) {  //TODO dont allow right ?
                throw new ClientException("Can't change Deleted Unwanted Customer", ErrorCode.APPLICATION_ERROR);
            }
            Optional<UnwantedCustomerReason> reasonOptional =
                    unwantedCustomerReasonRepository.findById(request.getUnwantedCustomerReasonId());
            if (reasonOptional.isPresent()) {
                UnwantedCustomerReason unwantedCustomerReason = reasonOptional.get();
                if (!unwantedCustomerReason.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                    unwantedCustomer.setName(request.getName());
                    unwantedCustomer.setAdditionalInfo(request.getAdditionalInfo());
                    unwantedCustomer.setUnwantedCustomerReasonId(request.getUnwantedCustomerReasonId());
                    unwantedCustomer.setCreateContractRestriction(request.getContractCreateRestriction());
                    unwantedCustomer.setCreateOrderRestriction(request.getOrderCreateRestriction());
                    unwantedCustomer.setStatus(request.getUnwantedCustomerStatus());
                    unwantedCustomer.setModifyDate(new Date());
                    unwantedCustomer.setModifySystemUserId("SysUser");//TODO sysUser add
                    return new UnwantedCustomerResponse(unwantedCustomerRepository.save(unwantedCustomer), unwantedCustomerReason.getName());
                } else {
                    throw new ClientException("Unwanted Customer reason is inactive", ErrorCode.APPLICATION_ERROR);
                }
            } else {
                throw new ClientException("Unwanted Customer reason not exists", ErrorCode.APPLICATION_ERROR);
            }
        } else {
            throw new ClientException("Unwanted Customer with this Id not found", ErrorCode.APPLICATION_ERROR);
        }
    }

    public UnwantedCustomerResponse view(Long id) {
        log.debug("Getting info of unwanted customer with id: {}", id);
        Optional<UnwantedCustomer> unwantedCustomerOptional = unwantedCustomerRepository.findById(id);
        if (unwantedCustomerOptional.isPresent()) {
            UnwantedCustomer unwantedCustomer = unwantedCustomerOptional.get();
            Optional<UnwantedCustomerReason> reasonOptional =
                    unwantedCustomerReasonRepository.findById(unwantedCustomer.getUnwantedCustomerReasonId());
            if(reasonOptional.isPresent()){
                return new UnwantedCustomerResponse(unwantedCustomer,reasonOptional.get().getName());
            } else {
                throw new ClientException("Unwanted Customer reason with this id doesn't exists",
                        ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
        } else throw new ClientException("Unwanted Customer with this id doesn't exists",
                ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
    }

    public Page<UnwantedCustomerResponse> filter(UnwantedCustomerFilterRequest request) {
        Sort.Order order = new Sort.Order(request.getDirection(), checkSortField(request));
        return unwantedCustomerRepository.filter(
                request.getPrompt(),
                checkFilterField(request.getFilterField()),
                request.getReasonId(),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order)));
    }

    private String checkSortField(UnwantedCustomerFilterRequest request) {
        if (request.getSortField() == null) {
            return UnwantedCustomerSortField.IDENTIFIER.getValue(); //doesn't take null or empty values
        } else return request.getSortField().getValue();
    }

    private String checkFilterField(Object object) {//TODO sysUser add
        if (object == null) {
            return "ALL";
        }
        return object.toString();
    }

    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting unwanted customer: {}", id);
        Optional<UnwantedCustomer> unwantedCustomerOptional = unwantedCustomerRepository.findById(id);
        if (unwantedCustomerOptional.isPresent()) {
            UnwantedCustomer unwantedCustomer = unwantedCustomerOptional.get();
            if (unwantedCustomer.getStatus().equals(UnwantedCustomerStatus.DELETED)) {
                throw new ClientException("Can't delete already deleted Unwanted Customer", ErrorCode.APPLICATION_ERROR);
            }
            unwantedCustomer.setStatus(UnwantedCustomerStatus.DELETED);
            unwantedCustomer.setModifyDate(new Date());
            unwantedCustomer.setModifySystemUserId("SysUser");//TODO sysUser add
            return unwantedCustomerRepository.save(unwantedCustomer).getId();
        } else {
            throw new ClientException("Unwanted Customer with this Id not found", ErrorCode.APPLICATION_ERROR);
        }
    }

    public UnwantedCustomer checkUnwantedCustomer(String identificationNumber) {
        log.debug("Checking unwanted customer with identificationNumber: {}", identificationNumber);
        Optional<UnwantedCustomer> unwantedCustomerOptional =
                unwantedCustomerRepository.findByIdentifier(identificationNumber);
        return unwantedCustomerOptional.orElse(null);
    }
}
