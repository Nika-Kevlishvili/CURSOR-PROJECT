package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.documentModels.contract.response.ManagerProxyResponse;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractProxyManagers;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface ServiceContractProxyManagersRepository extends JpaRepository<ServiceContractProxyManagers,Long> {
    List<ServiceContractProxyManagers> findByContractProxyIdAndStatusIn(Long id, List<ContractSubObjectStatus> statuses);

    List<ServiceContractProxyManagers> findByContractProxyIdAndStatus(Long id, ContractSubObjectStatus contractSubObjectStatus);

    List<ServiceContractProxyManagers> findByContractProxyIdAndIdNotInAndStatus(Long id, Set<Long> shouldBeUpdated, ContractSubObjectStatus contractSubObjectStatus);
    @Query(nativeQuery = true, value = """
            with sc_data as (select scd.customer_detail_id, scd.id
                             from service_contract.contract_details scd
                                      join service_contract.contracts sc on scd.contract_id = sc.id
                             where sc.id = :id
                               and scd.version_id = :versionId),
                 proxy_data as (select proxy.*
                                from service_contract.contract_proxies proxy
                                         join sc_data cd on proxy.contract_detail_id = cd.id
                                where proxy.status = 'ACTIVE'),
                 proxy_managers as (select manager.customer_manager_id,
                                           pd.*
                                    from service_contract.contract_proxy_managers manager
                                             join proxy_data pd on manager.contract_proxy_id = pd.id
                                    where manager.status = 'ACTIVE')
            select coalesce(proxy_info.proxy_by_proxy_name, proxy_info.proxy_name) as ProxyName,
                   coalesce(proxy_info.proxy_by_proxy_attorney_power_number,
                            proxy_info.proxy_attorney_power_number)                as PowerAttroneyNumber,
                   coalesce(proxy_info.proxy_by_proxy_notary_public,
                            proxy_info.proxy_notary_public)                        as NotaryPublic,
                   coalesce(proxy_info.proxy_by_proxy_operation_area,
                            proxy_info.proxy_operation_area)                       as OperationArea,
                coalesce(proxy_info.proxy_registration_number,
                            proxy_info.proxy_by_proxy_registration_number)                       as RegistrationNumber,
                   cm.id                                                           as ManagerId
            from customer.customer_managers cm
                     join customer.customer_details cd
                          on cm.customer_detail_id = cd.id
                     join sc_data scd on scd.customer_detail_id = cd.id
                     join proxy_managers proxy_info on proxy_info.customer_manager_id = cm.id
            """)
    List<ManagerProxyResponse> fetchManagerProxiesForDocument(Long id, Long versionId);

}
