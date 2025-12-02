package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.apis.service.ApisService;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.customer.UnwantedCustomer;
import bg.energo.phoenix.model.entity.nomenclature.customer.UnwantedCustomerReason;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerSortField;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.unwantedCustomer.UnwantedCustomerCreateRequest;
import bg.energo.phoenix.model.request.customer.unwantedCustomer.UnwantedCustomerEditRequest;
import bg.energo.phoenix.model.request.customer.unwantedCustomer.UnwantedCustomerFilterRequest;
import bg.energo.phoenix.model.response.customer.UnwantedCustomer.UnwantedCustomerCheckResponse;
import bg.energo.phoenix.model.response.customer.UnwantedCustomer.UnwantedCustomerResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.UnwantedCustomerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.UnwantedCustomerReasonRepository;
import bg.energo.phoenix.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnwantedCustomerService {
    private final UnwantedCustomerRepository unwantedCustomerRepository;
    private final ApisService apisService;
    private final UnwantedCustomerReasonRepository unwantedCustomerReasonRepository;
    private final PermissionService permissionService;

    /**
     * <h1>Create Unwanted Customer</h1>
     * then checks if unwanted customer reason is active else throws exception
     * if unwanted customer reason is active checks if unwanted customer identification number is in the database
     * if unwanted customer identification number is in the database throws exception
     * else checks if personal number is private customer if its private customer saves it as unwanted customer
     * else checks personal number in apis api - if user exists in the apis saves as unwanted customer else throws exception
     *
     * @param request   {@link UnwantedCustomerCreateRequest}
     * @return          created {@link UnwantedCustomerResponse}
     */
    public UnwantedCustomerResponse createUnwantedCustomer(UnwantedCustomerCreateRequest request) {
        log.debug("Creating unwanted customer: {}", request.toString());
        trimCreateRequestFields(request);

        String identificationNumber = request.getIdentificationNumber();
        UnwantedCustomer unwantedCustomer;

        UnwantedCustomerReason unwantedCustomerReason = unwantedCustomerReasonRepository
                .findByIdAndStatusIn(request.getUnwantedCustomerReasonId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("unwantedCustomerReasonId-Active unwanted customer reason with ID %s not found;".formatted(request.getUnwantedCustomerReasonId())));

        Optional<UnwantedCustomer> unwantedCustomerOptional = unwantedCustomerRepository.findByIdentifierAndStatusIn(identificationNumber, List.of(UnwantedCustomerStatus.ACTIVE));
        if (unwantedCustomerOptional.isPresent()) {
            log.error("Active unwanted customer with identification number {} already exists", identificationNumber);
            throw new ClientException("identificationNumber-Active Unwanted Customer with this UIC/Personal number is already added;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        unwantedCustomer = saveUnwantedCustomer(request);

        return new UnwantedCustomerResponse(unwantedCustomer,unwantedCustomerReason.getName());
    }

    /**
     * Trims all fields in unwanted customer create request.
     *
     * @param request unwanted customer create request
     */
    private void trimCreateRequestFields(UnwantedCustomerCreateRequest request) {
        request.setIdentificationNumber(request.getIdentificationNumber().trim());
        request.setName(request.getName().trim());
        request.setAdditionalInfo(StringUtils.isEmpty(request.getAdditionalInfo()) ? null : request.getAdditionalInfo().trim());
    }

    /**
     * <h1>saveUnwantedCustomer</h1>
     * saves unwanted customer to the database
     *
     * @param request
     * @return full entity object of saved unwanted customer
     */
    @Transactional
    public UnwantedCustomer saveUnwantedCustomer(UnwantedCustomerCreateRequest request) {
        UnwantedCustomer unwantedCustomer = new UnwantedCustomer();
        unwantedCustomer.setIdentifier(request.getIdentificationNumber());
        unwantedCustomer.setName(request.getName());
        unwantedCustomer.setUnwantedCustomerReasonId(request.getUnwantedCustomerReasonId());
        unwantedCustomer.setAdditionalInfo(request.getAdditionalInfo());
        unwantedCustomer.setCreateContractRestriction(request.getContractCreateRestriction());
        unwantedCustomer.setCreateOrderRestriction(request.getOrderCreateRestriction());
        unwantedCustomer.setStatus(UnwantedCustomerStatus.ACTIVE);
        return unwantedCustomerRepository.save(unwantedCustomer);
    }

    /**
     * <h1>CheckPrivateCustomer</h1>
     * if identification number length is 10 or 12 its private customer and returns true
     * else returns false
     *
     * @param identificationNumber  UIC/Personal number of the customer
     * @return boolean privateCustomerStatus
     */
    private Boolean checkPrivateCustomer(String identificationNumber) {
        return identificationNumber.length() == 10 || identificationNumber.length() == 12;
    }

    /**
     * <h1>Unwanted customer edit</h1>
     * function gets unwanted customer form the db by id
     * if customer exists continues, else throws exception
     * then checks unwanted customer status,continues if status is not DELETED
     * gets unwanted customer reason and if its not Inactive continues , else throws exception
     * then it maps request object to the entity object and saves it
     *
     * @param id      unwanted customer db id
     * @param request   {@link UnwantedCustomerEditRequest}
     * @return @return {@link UnwantedCustomerResponse}
     */
    @Transactional
    public UnwantedCustomerResponse edit(Long id, UnwantedCustomerEditRequest request) {
        log.debug("Editing unwanted customer: {}", request.toString());
        trimEditRequestFields(request);

        UnwantedCustomer unwantedCustomer = unwantedCustomerRepository
                .findByIdAndStatuses(id, List.of(UnwantedCustomerStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Active unwanted customer with ID %s not found;".formatted(id)));

        UnwantedCustomerReason unwantedCustomerReason = unwantedCustomerReasonRepository
                .findByIdAndStatusIn(request.getUnwantedCustomerReasonId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("unwantedCustomerReasonId-Active/inactive unwanted customer reason with ID %s not found;"
                                                                             .formatted(request.getUnwantedCustomerReasonId())));

        if (unwantedCustomerReason.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
            if (!unwantedCustomerReason.getId().equals(unwantedCustomer.getUnwantedCustomerReasonId())) {
                log.error("Nomenclature is inactive and it is different from the persisted one.");
                throw new ClientException(
                        "unwantedCustomerReasonId-Nomenclature with ID %s is inactive and it is different from the persisted one;"
                                .formatted(request.getUnwantedCustomerReasonId()),
                        ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
        }

        unwantedCustomer.setName(request.getName());
        unwantedCustomer.setAdditionalInfo(request.getAdditionalInfo());
        unwantedCustomer.setUnwantedCustomerReasonId(request.getUnwantedCustomerReasonId());
        unwantedCustomer.setCreateContractRestriction(request.getContractCreateRestriction());
        unwantedCustomer.setCreateOrderRestriction(request.getOrderCreateRestriction());
        unwantedCustomer.setStatus(request.getUnwantedCustomerStatus());
        return new UnwantedCustomerResponse(unwantedCustomerRepository.save(unwantedCustomer), unwantedCustomerReason.getName());
    }

    /**
     * Trims all fields in unwanted customer edit request.
     *
     * @param request   unwanted customer edit request
     */
    private void trimEditRequestFields(UnwantedCustomerEditRequest request) {
        request.setName(request.getName().trim());
        request.setAdditionalInfo(StringUtils.isEmpty(request.getAdditionalInfo()) ? null : request.getAdditionalInfo().trim());
    }

    /**
     * <h1>Unwanted Customer view</h1>
     * function takes id , checks it in the database and if it exists return it
     *
     * @param id unwanted customer db id
     * @return @return {@link UnwantedCustomerResponse}
     */
    public UnwantedCustomerResponse view(Long id) {
        log.debug("Getting info of unwanted customer with id: {}", id);
        Optional<UnwantedCustomer> unwantedCustomerOptional = unwantedCustomerRepository.findByIdAndStatuses(id, getViewStatus());
        if (unwantedCustomerOptional.isPresent()) {
            UnwantedCustomer unwantedCustomer = unwantedCustomerOptional.get();
            Optional<UnwantedCustomerReason> reasonOptional =
                    unwantedCustomerReasonRepository.findById(unwantedCustomer.getUnwantedCustomerReasonId());
            if(reasonOptional.isPresent()){
                return new UnwantedCustomerResponse(unwantedCustomer,reasonOptional.get().getName());
            } else {
                throw new ClientException("id-Unwanted Customer reason with this id doesn't exists",
                        ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
        } else throw new ClientException("id-Unwanted Customer with this id doesn't exists",
                ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
    }

    /**
     * <h1>Unwanted customer filter</h1>
     * function selects,orders and sorts unwanted customer list and returns it as page list
     *
     * @param request   {@link UnwantedCustomerFilterRequest}
     * @return @return {@link UnwantedCustomerResponse} page list
     */
    public Page<UnwantedCustomerResponse> filter(UnwantedCustomerFilterRequest request) {
        log.debug("Filtering unwanted customer list: {}", request.toString());

        Sort.Order order = new Sort.Order(request.getDirection(), checkSortField(request));
        return unwantedCustomerRepository
                .filter(
                        checkPrompt(request.getPrompt()),
                        checkFilterField(request.getFilterField()),
                        request.getReasonId(),
                        getViewStatus(),
                        PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))
                );
    }

    /**
     * <h1>Unwanted customer Filter checkPrompt</h1>
     * checks request prompt key for null value
     * if it's not null , transforms value to the lowercase letters
     *
     * @param prompt
     * @return String prompt
     */
    private String checkPrompt(String prompt) {
        String returnString = null;
        if(prompt != null){
            returnString = prompt.toLowerCase();
        }
        return returnString;
    }

    /**
     * <h1>Unwanted customer Filter checkSortField</h1>
     * checks request sortField for null values
     * if its null return IDENTIFIER sorting as Default
     * else return sortFieldValue
     *
     * @param request
     * @return String sortField
     */
    private String checkSortField(UnwantedCustomerFilterRequest request) {
        if (request.getSortField() == null) {
            return UnwantedCustomerSortField.IDENTIFIER.getValue(); //doesn't take null or empty values
        } else return request.getSortField().getValue();
    }

    /**
     * <h1>Unwanted customer Filter checkFilterField</h1>
     * checks request filterField for null values
     * if its null return ALL as Default
     * else return filterField String value
     *
     * @param object
     * @return String filterFieldValue
     */
    private String checkFilterField(Object object) {//TODO sysUser add
        if (object == null) {
            return "ALL";
        }
        return object.toString();
    }

    /**
     * <h1>Unwanted Customer Delete</h1>
     * function selects unwanted customer from the database
     * if it exists and the status is not DELETED
     * assigns DELETED status to the record and saves it
     *
     * @param id Unwanted customer db id
     * @return Unwanted customer db id
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting unwanted customer: {}", id);
        Optional<UnwantedCustomer> unwantedCustomerOptional = unwantedCustomerRepository.findById(id);
        if (unwantedCustomerOptional.isPresent()) {
            UnwantedCustomer unwantedCustomer = unwantedCustomerOptional.get();
            if (unwantedCustomer.getStatus().equals(UnwantedCustomerStatus.DELETED)) {
                throw new ClientException("id-Can't delete already deleted Unwanted Customer", ErrorCode.APPLICATION_ERROR);
            }
            unwantedCustomer.setStatus(UnwantedCustomerStatus.DELETED);
            return unwantedCustomerRepository.save(unwantedCustomer).getId();
        } else {
            throw new ClientException("id-Unwanted Customer with this Id not found", ErrorCode.APPLICATION_ERROR);
        }
    }

    /**
     * <h1>Unwanted customer checkIfExists</h1>
     * takes personal number , calls checkUnwantedCustomer function and returns result
     *
     * @param identificationNumber personal number of the customer
     * @return @return {@link UnwantedCustomerCheckResponse}
     */
    public UnwantedCustomerCheckResponse checkIfExists(String identificationNumber) {
        UnwantedCustomer unwantedCustomer = checkUnwantedCustomer(identificationNumber);
        if(unwantedCustomer != null){
            return new UnwantedCustomerCheckResponse(unwantedCustomer.getId());
        } else return new UnwantedCustomerCheckResponse(null);
    }

    /**
     * <h1>Unwanted customer checkUnwantedCustomer</h1>
     * takes personal number , selects from database if exists returns it else returns null
     *
     * @param identificationNumber  personal number of the customer to check
     * @return @return {@link UnwantedCustomer}
     */
    public UnwantedCustomer checkUnwantedCustomer(String identificationNumber) {
        log.debug("Checking unwanted customer with identificationNumber: {}", identificationNumber);
        Optional<UnwantedCustomer> unwantedCustomerOptional = unwantedCustomerRepository
                .findByIdentifierAndStatusIn(identificationNumber, List.of(UnwantedCustomerStatus.ACTIVE));
        return unwantedCustomerOptional.orElse(null);
    }

    /**
     * <h1>Unwanted customer getViewStatus</h1>
     * gets permissions of the logged in user
     * checks if UC_VIEW_DELETED permission exists and returns statuses accordingly
     *
     * @return list of @return {@link UnwantedCustomerStatus} object
     */
    private List<UnwantedCustomerStatus> getViewStatus() {
        List<UnwantedCustomerStatus> statuses = new ArrayList<>();
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.UC);

        if(context.contains(PermissionEnum.UC_VIEW_DELETED.getId())){
            statuses.add(UnwantedCustomerStatus.DELETED);
        }

        if (context.contains(PermissionEnum.UC_VIEW_BASIC.getId())) {
            statuses.add(UnwantedCustomerStatus.ACTIVE);
        }
        return statuses;
    }
}
