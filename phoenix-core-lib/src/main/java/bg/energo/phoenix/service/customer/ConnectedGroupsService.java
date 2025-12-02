package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.customer.ConnectedGroup;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerConnectedGroup;
import bg.energo.phoenix.model.entity.nomenclature.customer.GccConnectionType;
import bg.energo.phoenix.model.enums.GCCSortBy;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.ConnectedGroupFilterRequest;
import bg.energo.phoenix.model.request.customer.ConnectedGroupRequest;
import bg.energo.phoenix.model.response.customer.ConnectedGroupCustomerResponse;
import bg.energo.phoenix.model.response.customer.ConnectedGroupDetailResponse;
import bg.energo.phoenix.model.response.customer.ConnectedGroupFilterResponse;
import bg.energo.phoenix.model.response.customer.ConnectedGroupResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.ConnectedGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerConnectedGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.GccConnectionTypeRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectedGroupsService {

    private final ConnectedGroupRepository connectedGroupRepository;
    private final CustomerConnectedGroupRepository connectionRepository;
    private final CustomerRepository customerRepository;
    private final GccConnectionTypeRepository gccConnectionTypeRepository;
    private final PermissionService permissionService;

    @Transactional
    public ConnectedGroupResponse create(ConnectedGroupRequest request) {
        log.debug("creating Group of connected customers {}", request);

        if (connectedGroupRepository.existsByNameAndStatusInAndIdNot(request.getGroupName(), List.of(Status.ACTIVE), null)) {
            log.debug("groupName-Group exists with same name");
            throw new ClientException("groupName-Group with such name already exists;", ErrorCode.CONFLICT);
        }

        ConnectedGroup connectedGroup = new ConnectedGroup(request);
        GccConnectionType gccConnectionType = gccConnectionTypeRepository
                .findByIdAndStatus(request.getConnectionTypeId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("connectionTypeId-Connection type with id %s not exists", request.getConnectionTypeId())));
        connectedGroup.setGccConnectionType(gccConnectionType);
        ConnectedGroup savedGroup = connectedGroupRepository.save(connectedGroup);

        List<CustomerConnectedGroup> customerConnectedGroups = request
                .getCustomerIds()
                .stream()
                .map(customerId -> {
                            Customer customer = customerRepository
                                    .findByIdAndStatuses(customerId, List.of(CustomerStatus.ACTIVE))
                                    .orElseThrow(() -> new DomainEntityNotFoundException(String.format("customerIds-Customer with customerId %s not found", customerId)));
                            return new CustomerConnectedGroup(customer.getId(), Status.ACTIVE, savedGroup.getId());
                        }
                )
                .toList();
        connectionRepository.saveAll(customerConnectedGroups);

        return new ConnectedGroupResponse(savedGroup);
    }

    public Page<ConnectedGroupResponse> list(String prompt, Integer page, Integer size) {
        log.debug("listing Groups of connected customers {} {} {}", prompt, page, size);
        return connectedGroupRepository
                .filterByNameAndStatus(
                        StringUtil.underscoreReplacer(prompt),
                        getStatusesByPermissions(),
                        PageRequest.of(page, size)
                )
                .map(ConnectedGroupResponse::new);
    }

    public ConnectedGroupDetailResponse get(Long id) {
        log.debug("getting Group of connected customers {}", id);
        ConnectedGroup connectedGroup = connectedGroupRepository
                .findByIdAndStatus(id, getStatusesByPermissions())
                .orElseThrow(() -> new ClientException(String.format("id-GCC with id %s not found", id), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        List<ConnectedGroupCustomerResponse> byConnectedGroupId = connectionRepository.findCustomerInfoByGroupId(id, List.of(Status.ACTIVE));
        ConnectedGroupDetailResponse connectedGroupDetailResponse = new ConnectedGroupDetailResponse(connectedGroup);
        connectedGroupDetailResponse.setCustomerInfos(byConnectedGroupId);
        return connectedGroupDetailResponse;
    }

    @Transactional
    public void remove(Long id) {
        log.debug("removing Group of connected customers {}", id);

        ConnectedGroup connectedGroup = connectedGroupRepository
                .findByIdAndStatus(id, List.of(Status.ACTIVE))
                .orElseThrow(() -> new ClientException(String.format("id-GCC with id %s not found", id), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        List<CustomerConnectedGroup> connections = connectionRepository.findByConnectedGroupId(id, List.of(Status.ACTIVE));
        if (!CollectionUtils.isEmpty(connections)) {
            log.error("id-GCC with id {} has attached customers", id);
            throw new ClientException(String.format("id-GCC with id %s has attached customers", id), ErrorCode.OPERATION_NOT_ALLOWED);
        }
        connectedGroup.setStatus(Status.DELETED);

        connectedGroupRepository.save(connectedGroup);
    }

    @Transactional
    public void edit(Long id, ConnectedGroupRequest request) {
        log.debug("editing Group of connected customers {}", id);

        ConnectedGroup connectedGroup = connectedGroupRepository
                .findByIdAndStatus(id, List.of(Status.ACTIVE))
                .orElseThrow(() -> new ClientException(String.format("id-GCC with id %s not found", id), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (connectedGroupRepository.existsByNameAndStatusInAndIdNot(request.getGroupName(), List.of(Status.ACTIVE), connectedGroup.getId())) {
            log.debug("groupName-Group exists with same name");
            throw new ClientException("groupName-Group with such name already exists;", ErrorCode.CONFLICT);
        }

        GccConnectionType gccConnectionType = gccConnectionTypeRepository
                .findByIdAndStatus(request.getConnectionTypeId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new ClientException(String.format("connectionTypeId-Connection type with id %s not exists", request.getConnectionTypeId()), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        List<CustomerConnectedGroup> connections = connectionRepository
                .findByConnectedGroupId(id, List.of(Status.ACTIVE));

        List<Long> customerIds = request.getCustomerIds();

        Map<Long, CustomerConnectedGroup> connectionMap = connections
                .stream()
                .collect(Collectors.toMap(CustomerConnectedGroup::getCustomerId, c -> c));

        connectedGroup.setName(request.getGroupName());
        connectedGroup.setAdditionalInfo(request.getAdditionalInformation());
        connectedGroup.setGccConnectionType(gccConnectionType);
        createConnection(customerIds, connectedGroup, connections, connectionMap);
        deleteConnection(request, connectionMap);

        connectionRepository.saveAll(connections);
        connectedGroupRepository.save(connectedGroup);
    }

    private void createConnection(List<Long> customerIds, ConnectedGroup connectedGroup, List<CustomerConnectedGroup> connections, Map<Long, CustomerConnectedGroup> connectionMap) {
        if (customerIds == null) {
            return;
        }

        customerIds.stream()
                .filter(x -> !connectionMap.containsKey(x))
                .forEach(customerId -> {
                    Customer customer = customerRepository
                            .findByIdAndStatuses(customerId, List.of(CustomerStatus.ACTIVE))
                            .orElseThrow(() -> new DomainEntityNotFoundException(String.format("customerIds-Customer with id %s not found", customerId)));
                    CustomerConnectedGroup customerConnectedGroup = new CustomerConnectedGroup(customer.getId(), Status.ACTIVE, connectedGroup.getId());
                    connections.add(customerConnectedGroup);
                });
    }

    private void deleteConnection(ConnectedGroupRequest request, Map<Long, CustomerConnectedGroup> connectionMap) {
        connectionMap.keySet().stream()
                .filter(x -> !request.getCustomerIds().contains(x))
                .forEach(customerId -> {
                    CustomerConnectedGroup customerConnectedGroup = connectionMap.get(customerId);
                    customerConnectedGroup.setStatus(Status.DELETED);
                });
    }


    public List<ConnectedGroupResponse> listForCustomer(Customer customer) {
        Optional.ofNullable(customer)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-ConnectedGroup: Customer do not exists!"));

        return connectedGroupRepository
                .findByCustomerIdAndStatus(customer.getId(), List.of(Status.ACTIVE))
                .stream()
                .map(ConnectedGroupResponse::new)
                .toList();
    }

    public Page<ConnectedGroupFilterResponse> filter(ConnectedGroupFilterRequest request) {
        Sort sort;

        if (request.getSortBy().equals(GCCSortBy.NUMBER_CONNECTIONS)) {
            sort = JpaSort.unsafe(
                    request.getDirection(),
                    "(select count(1) from customer.customer_connected_groups con where con.connected_group_id = g.id and con.status = 'ACTIVE')"
            );
        } else {
            sort = JpaSort.by(request.getDirection(), request.getSortBy().getValue());
        }

        return connectedGroupRepository
                .filter(
                        request.getSearchField() == null ? null : request.getSearchField().name(),
                        StringUtil.underscoreReplacer(request.getSearchValue()),
                        request.getConnectionId() == null ? new ArrayList<>() : request.getConnectionId(),
                        request.getCustomerCountFrom(),
                        request.getCustomerCountTo(),
                        getStatusesByPermissions().stream().map(Enum::name).toList(),
                        PageRequest.of(request.getPage(), request.getSize(), sort)
                );
    }

    /**
     * @return list of statuses that are allowed to be viewed by the current user
     */
    private List<Status> getStatusesByPermissions() {
        List<Status> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.GCC, List.of(PermissionEnum.GCC_VIEW_BASIC))) {
            statuses.add(Status.ACTIVE);
        }
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.GCC, List.of(PermissionEnum.GCC_VIEW_DELETED))) {
            statuses.add(Status.DELETED);
        }
        return statuses;
    }
}
