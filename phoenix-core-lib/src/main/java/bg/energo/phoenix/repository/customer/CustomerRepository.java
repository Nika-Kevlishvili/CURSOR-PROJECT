package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForCustomer;
import bg.energo.phoenix.model.documentModels.latePaymentFine.CommunicationDataMiddleResponse;
import bg.energo.phoenix.model.documentModels.mlo.Manager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.CustomerListingResponse;
import bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse;
import bg.energo.phoenix.model.response.contract.action.ActionCustomerResponse;
import bg.energo.phoenix.model.response.crm.smsCommunication.ActiveContractsAndAssociatedCustomersForMassCommunicationProjection;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactPurposeMiddleResponse;
import bg.energo.phoenix.model.response.customer.customerRelated.relationship.CustomerRelatedRelationshipMiddleResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findFirstByIdentifierAndStatusIn(@Param("identifier") String identifier,
                                                        @Param("status") List<CustomerStatus> status);

    Optional<Customer> findByCustomerNumberAndStatus(
            @Param("customerNumber") Long customerNumber,
            @Param("status") CustomerStatus status
    );

    @Query(value = """
                   select new bg.energo.phoenix.model.request.customer.CustomerListingResponse(
                        c.identifier,
                        cd.status,
                        c.customerType,
                        cd.name,
                        cd.middleName,
                        cd.lastName,
                        lf.name,
                        (case when :displayNameDirection = 'ASC' then vwc.displayName when :displayNameDirection = 'DESC' then vwc.displayNameDesc else vwc.displayName end) as displayName,
                        (case when :canEditCustomer is null then 'false'
                            else (select coalesce(MAX('true'),'false')
                            from CustomerAccountManager cam
                            join AccountManager am on am.id = cam.managerId
                                where cam.customerDetail.id = cd.id
                                and cam.status = 'ACTIVE'
                                and am.userName = :canEditCustomer) end) as canEditAsManager,
                        cd.economicBranchCiId,
                        ebc.name as economic_branch_name,
                        coalesce(pp.id, cd.id),
                        coalesce(pp.name,cd.populatedPlaceForeign) as populated_place_name,
                        cd.createDate,
                        case when uc.id is null then 'NO' else 'YES' end as unwanted_customer_status,
                        c.id,
                        cd.id,
                        c.status
                   )
                   from Customer c
                   join CustomerDetails cd on c.lastCustomerDetailId = cd.id
                   left join LegalForm lf on cd.legalFormId = lf.id
                   left join EconomicBranchCI ebc on cd.economicBranchCiId = ebc.id
                   left join PopulatedPlace pp on cd.populatedPlaceId = pp.id
                   left join UnwantedCustomer uc on uc.identifier = c.identifier and uc.status ='ACTIVE'
                   left join VwCustomerAccountManager vwc on  vwc.customerDetailId = cd.id
                        where c.id in (select c.id as customer_id
                            from Customer c
                            join CustomerDetails cd on cd.customerId = c.id
                                where (coalesce(:customertype,'0') = '0' or  c.customerType in :customertype)
                                and (coalesce(:status,'0') = '0' or  cd.status in (:status))
                                and (coalesce(:economicbranch,'0') = '0' or  cd.economicBranchCiId  in :economicbranch)
                                and (:isunwantedcustomer = 'ALL' or (select coalesce(MAX('YES'),'NO')
                                    from UnwantedCustomer uc
                                    where uc.identifier = c.identifier
                                    and uc.status = 'ACTIVE') = :isunwantedcustomer)
                                 and (exists(select 1 from CustomerAccountManager cam where cam.customerDetail.id = cd.id and cam.managerId in (:customeraccountmanager) and cam.status = 'ACTIVE') or :customeraccountmanager is null)
                                and (:populatedplace is null or exists (select pp3.id from PopulatedPlace pp3
                                    where cd.populatedPlaceId = pp3.id 
                                    and lower(pp3.name) like :populatedplace)
                                    or (lower(cd.populatedPlaceForeign) like :populatedplace or :populatedplace is null))
                                and c.status in (:customerStatuses)
                                and (:systemUserId is null or exists (select 1 from AccountManager am join CustomerAccountManager ccmm on ccmm.managerId = am.id where ccmm.customerDetail.id = cd.id and am.userName = :systemUserId))
                                and (coalesce(:excludepastversion,'false') = 'false' or (:excludepastversion = 'true' and cd.id = c.lastCustomerDetailId))
                                and (:columnname is null
                                    or (:columnname = 'ALL'
                                        and (lower(c.identifier) like :columnvalue
                                        or lower(cd.name) like :columnvalue
                                        or lower(cd.middleName) like :columnvalue
                                        or lower(cd.lastName) like :columnvalue
                                        or lower(concat(cd.name, ' ', cd.middleName, ' ', cd.lastName)) like :columnvalue
                                        or lower(concat(cd.name, ' ', cd.middleName)) like :columnvalue
                                        or lower(concat(cd.name, ' ', cd.lastName)) like :columnvalue
                                        or lower(concat(cd.middleName, ' ', cd.lastName)) like :columnvalue
                                        or exists (select cn.id from Country cn where cd.countryId  = cn.id and lower(cn.name) like :columnvalue)
                                        or (exists (select r.id
                                            from Region r join Municipality m on m.region.id  = r.id
                                            join PopulatedPlace pp1 on pp1.municipality.id = m.id
                                                where pp1.id  = cd.populatedPlaceId
                                                and lower(r.name) like :columnvalue)
                                                or lower(cd.regionForeign) like :columnvalue)
                                                or (exists (select m.id from Municipality m join PopulatedPlace pp2 on pp2.municipality.id = m.id where pp2.id  = cd.populatedPlaceId and lower(m.name) like :columnvalue) or lower(cd.municipalityForeign) like :columnvalue)
                                                or (exists (select d.id from District d where cd.districtId = d.id and lower(d.name) like :columnvalue) or lower(cd.districtForeign) like :columnvalue)
                                                or (exists (select s.id from Street s where cd.streetId = s.id and lower(s.name) like :columnvalue) or lower(cd.streetForeign) like :columnvalue)
                                                or (exists (select ra.id from ResidentialArea ra where cd.residentialAreaId = ra.id and lower(ra.name) like :columnvalue) or lower(cd.ResidentialAreaForeign) like :columnvalue)
                                                or exists (select 1 from Manager cm where cm.customerDetailId = cd.id and (lower(cm.name) like :columnvalue or lower(cm.middleName) like :columnvalue or lower(cm.surname) like :columnvalue))
                                                or exists (select 1 from CustomerAccountManager cam2, AccountManager am where cam2.customerDetail.id = cd.id and cam2.managerId = am.id
                                                        and (lower(am.displayName) like :columnvalue or lower(am.userName) like :columnvalue or lower(am.organizationalUnit) like :columnvalue or lower(am.businessUnit) like :columnvalue)))
                                    )
                                    or (
                                        (:columnname = 'CUSTOMERNAME' and lower(cd.name) like :columnvalue)
                                        or (:columnname = 'NAME_OF_LEGAL_ENTITY' and lower(cd.name) like :columnvalue)
                                        or (:columnname = 'IDENTIFIER' and lower(c.identifier) like :columnvalue)
                                        or (:columnname = 'PERSONAL_NUMBER' and lower(c.identifier) like :columnvalue)
                                        or (:columnname = 'CUSTOMERMIDDLENAME' and lower(cd.middleName)  like :columnvalue)
                                        or (:columnname = 'CUSTOMERLASTENAME' and lower(cd.lastName)  like :columnvalue)
                                        or (:columnname = 'COUNTRY' and  exists (select cn.id from Country cn where cd.countryId  = cn.id and lower(cn.name) like :columnvalue))
                                        or (:columnname = 'REGION'
                                            and (exists (select r.id
                                                from Region r join Municipality m on m.region.id  = r.id
                                                join PopulatedPlace pp1 on pp1.municipality.id = m.id
                                                    where pp1.id  = cd.populatedPlaceId
                                                    and lower(r.name) like :columnvalue )
                                                    or lower(cd.regionForeign) like :columnvalue))
                                        or (:columnname =  'MUNICIPALITY'
                                            and (exists (select m.id from Municipality m
                                            join PopulatedPlace pp2 on pp2.municipality.id = m.id
                                                where pp2.id = cd.populatedPlaceId
                                                and lower(m.name) like :columnvalue)
                                                or lower(cd.municipalityForeign)  like :columnvalue))
                                        or (:columnname = 'DISTRICT'
                                            and (exists (select d.id from District d
                                            where cd.districtId = d.id
                                            and lower(d.name) like :columnvalue)
                                            or lower(cd.districtForeign)  like :columnvalue))
                                        or (:columnname = 'STREET'
                                            and (exists (select s.id from Street s
                                            where cd.streetId = s.id
                                            and lower(s.name) like :columnvalue)
                                            or lower(cd.streetForeign)  like :columnvalue))
                                        or (:columnname = 'RESIDENTIALAREA'
                                            and (exists (select ra.id from ResidentialArea ra  
                                            where cd.residentialAreaId  = ra.id
                                            and lower(ra.name) like :columnvalue)
                                            or lower(cd.ResidentialAreaForeign) like :columnvalue))
                                        or (:columnname = 'CUSTOMERMANAGER_NAME'
                                            and exists (select 1 from Manager cm 
                                            where cm.customerDetailId = cd.id
                                            and cm.status = 'ACTIVE'
                                            and (lower(cm.name) like :columnvalue)))
                                        or (:columnname = 'CUSTOMERMANAGER_MIDDLE_NAME'
                                            and exists (select 1 from Manager cm
                                            where cm.customerDetailId = cd.id
                                            and cm.status = 'ACTIVE'
                                            and (lower(cm.middleName) like :columnvalue)))
                                        or (:columnname = 'CUSTOMERMANAGER_LAST_NAME'
                                            and exists (select 1 from Manager cm
                                            where cm.customerDetailId = cd.id
                                            and cm.status = 'ACTIVE'
                                            and (lower(cm.surname) like :columnvalue)))
                                        or (:columnname = 'ACCOUNT_MANAGER_NAME'
                                            and exists (select 1 from CustomerAccountManager cam2, AccountManager am
                                            where cam2.customerDetail.id = cd.id
                                            and cam2.managerId = am.id
                                            and cam2.status = 'ACTIVE'
                                            and lower(am.userName) like :columnvalue))
                                        or (:columnname = 'ACCOUNT_MANAGER_ORG_UNIT'
                                            and exists (select 1 from CustomerAccountManager cam2, AccountManager am
                                            where cam2.customerDetail.id = cd.id
                                            and cam2.managerId = am.id
                                            and cam2.status = 'ACTIVE'
                                            and lower(am.organizationalUnit) like :columnvalue))
                                        or (:columnname = 'ACCOUNT_MANAGER_BUS_UNIT'
                                            and exists (select 1 from CustomerAccountManager cam2, AccountManager am
                                            where cam2.customerDetail.id = cd.id
                                            and cam2.managerId = am.id
                                            and cam2.status = 'ACTIVE'
                                            and lower(am.businessUnit) like :columnvalue))
                                        or (:columnname = 'ACCOUNT_MANAGER_DISPLAY_NAME'
                                            and exists (select 1 from CustomerAccountManager cam2, AccountManager am
                                            where cam2.customerDetail.id = cd.id
                                            and cam2.managerId = am.id
                                            and cam2.status = 'ACTIVE'
                                            and lower(am.displayName) like :columnvalue))
                                    )
                                )
                        )
            """,
            countQuery = """
                     select count(1) from Customer c
                    join CustomerDetails cd on c.lastCustomerDetailId = cd.id
                    left join LegalForm lf on cd.legalFormId = lf.id
                    left join EconomicBranchCI ebc on cd.economicBranchCiId = ebc.id
                    left join PopulatedPlace pp on cd.populatedPlaceId = pp.id
                    left join UnwantedCustomer uc on uc.identifier = c.identifier and uc.status ='ACTIVE'
                    left join VwCustomerAccountManager vwc on  vwc.customerDetailId = cd.id
                         where c.id in (select c.id as customer_id
                             from Customer c
                             join CustomerDetails cd on cd.customerId = c.id
                                 where (coalesce(:customertype,'0') = '0' or  c.customerType in :customertype)
                                 and (coalesce(:status,'0') = '0' or  cd.status in (:status))
                                 and (coalesce(:economicbranch,'0') = '0' or  cd.economicBranchCiId  in :economicbranch)
                                 and (:isunwantedcustomer = 'ALL' or (select coalesce(MAX('YES'),'NO')
                                     from UnwantedCustomer uc
                                     where uc.identifier = c.identifier
                                     and uc.status = 'ACTIVE') = :isunwantedcustomer)
                                 and (exists(select 1 from CustomerAccountManager cam where cam.customerDetail.id = cd.id and cam.managerId in (:customeraccountmanager) and cam.status = 'ACTIVE') or :customeraccountmanager is null)
                                 and (:populatedplace is null or exists (select pp3.id from PopulatedPlace pp3
                                     where cd.populatedPlaceId = pp3.id 
                                     and lower(pp3.name) like :populatedplace)
                                     or (lower(cd.populatedPlaceForeign) like :populatedplace or :populatedplace is null))
                                 and c.status in (:customerStatuses)
                                 and (:systemUserId is null or exists (select 1 from AccountManager am join CustomerAccountManager ccmm on ccmm.managerId = am.id where ccmm.customerDetail.id = cd.id and am.userName = :systemUserId))
                                 and (coalesce(:excludepastversion,'false') = 'false' or (:excludepastversion = 'true' and cd.id = c.lastCustomerDetailId))
                                 and (:columnname is null
                                     or (:columnname = 'ALL'
                                         and (lower(cd.name) like :columnvalue
                                         or lower(c.identifier) like :columnvalue
                                         or lower(cd.middleName) like :columnvalue
                                         or lower(cd.lastName) like :columnvalue
                                         or lower(concat(cd.name, ' ', cd.middleName, ' ', cd.lastName)) like :columnvalue
                                         or lower(concat(cd.name, ' ', cd.middleName)) like :columnvalue
                                         or lower(concat(cd.name, ' ', cd.lastName)) like :columnvalue
                                         or lower(concat(cd.middleName, ' ', cd.lastName)) like :columnvalue
                                         or exists (select cn.id from Country cn where cd.countryId  = cn.id and lower(cn.name) like :columnvalue)
                                         or (exists (select r.id
                                             from Region r join Municipality m on m.region.id  = r.id
                                             join PopulatedPlace pp1 on pp1.municipality.id = m.id
                                                 where pp1.id  = cd.populatedPlaceId
                                                 and lower(r.name) like :columnvalue)
                                                 or lower(cd.regionForeign) like :columnvalue)
                                                 or (exists (select m.id from Municipality m join PopulatedPlace pp2 on pp2.municipality.id = m.id where pp2.id  = cd.populatedPlaceId and lower(m.name) like :columnvalue) or lower(cd.municipalityForeign) like :columnvalue)
                                                 or (exists (select d.id from District d where cd.districtId = d.id and lower(d.name) like :columnvalue) or lower(cd.districtForeign) like :columnvalue)
                                                 or (exists (select s.id from Street s where cd.streetId = s.id and lower(s.name) like :columnvalue) or lower(cd.streetForeign) like :columnvalue)
                                                 or (exists (select ra.id from ResidentialArea ra where cd.residentialAreaId = ra.id and lower(ra.name) like :columnvalue) or lower(cd.ResidentialAreaForeign) like :columnvalue)
                                                 or exists (select 1 from Manager cm where cm.customerDetailId = cd.id and (lower(cm.name) like :columnvalue or lower(cm.middleName) like :columnvalue or lower(cm.surname) like :columnvalue))
                                                 or exists (select 1 from CustomerAccountManager cam2, AccountManager am where cam2.customerDetail.id = cd.id and cam2.managerId = am.id
                                                         and (lower(am.displayName) like :columnvalue or lower(am.userName) like :columnvalue or lower(am.organizationalUnit) like :columnvalue or lower(am.businessUnit) like :columnvalue)))
                                     )
                                     or (
                                         (:columnname = 'CUSTOMERNAME' and lower(cd.name) like :columnvalue)
                                         or (:columnname = 'NAME_OF_LEGAL_ENTITY' and lower(cd.name) like :columnvalue)
                                         or (:columnname = 'IDENTIFIER' and lower(c.identifier) like :columnvalue)
                                         or (:columnname = 'PERSONAL_NUMBER' and lower(c.identifier) like :columnvalue)
                                         or (:columnname = 'CUSTOMERMIDDLENAME' and lower(cd.middleName)  like :columnvalue)
                                         or (:columnname = 'CUSTOMERLASTENAME' and lower(cd.lastName)  like :columnvalue)
                                         or (:columnname = 'COUNTRY' and  exists (select cn.id from Country cn where cd.countryId  = cn.id and lower(cn.name) like :columnvalue))
                                         or (:columnname = 'REGION'
                                             and (exists (select r.id
                                                 from Region r join Municipality m on m.region.id  = r.id
                                                 join PopulatedPlace pp1 on pp1.municipality.id = m.id
                                                     where pp1.id  = cd.populatedPlaceId
                                                     and lower(r.name) like :columnvalue )
                                                     or lower(cd.regionForeign) like :columnvalue))
                                         or (:columnname =  'MUNICIPALITY'
                                             and (exists (select m.id from Municipality m
                                             join PopulatedPlace pp2 on pp2.municipality.id = m.id
                                                 where pp2.id = cd.populatedPlaceId
                                                 and lower(m.name) like :columnvalue)
                                                 or lower(cd.municipalityForeign)  like :columnvalue))
                                         or (:columnname = 'DISTRICT'
                                             and (exists (select d.id from District d
                                             where cd.districtId = d.id
                                             and lower(d.name) like :columnvalue)
                                             or lower(cd.districtForeign)  like :columnvalue))
                                         or (:columnname = 'STREET'
                                             and (exists (select s.id from Street s
                                             where cd.streetId = s.id
                                             and lower(s.name) like :columnvalue)
                                             or lower(cd.streetForeign)  like :columnvalue))
                                         or (:columnname = 'RESIDENTIALAREA'
                                             and (exists (select ra.id from ResidentialArea ra  
                                             where cd.residentialAreaId  = ra.id
                                             and lower(ra.name) like :columnvalue)
                                             or lower(cd.ResidentialAreaForeign) like :columnvalue))
                                         or (:columnname = 'CUSTOMERMANAGER_NAME'
                                             and exists (select 1 from Manager cm 
                                             where cm.customerDetailId = cd.id
                                             and cm.status = 'ACTIVE'
                                             and (lower(cm.name) like :columnvalue)))
                                         or (:columnname = 'CUSTOMERMANAGER_MIDDLE_NAME'
                                             and exists (select 1 from Manager cm
                                             where cm.customerDetailId = cd.id
                                             and cm.status = 'ACTIVE'
                                             and (lower(cm.middleName) like :columnvalue)))
                                         or (:columnname = 'CUSTOMERMANAGER_LAST_NAME'
                                             and exists (select 1 from Manager cm
                                             where cm.customerDetailId = cd.id
                                             and cm.status = 'ACTIVE'
                                             and (lower(cm.surname) like :columnvalue)))
                                         or (:columnname = 'ACCOUNT_MANAGER_NAME'
                                             and exists (select 1 from CustomerAccountManager cam2, AccountManager am
                                             where cam2.customerDetail.id = cd.id
                                             and cam2.managerId = am.id
                                             and cam2.status = 'ACTIVE'
                                             and lower(am.userName) like :columnvalue))
                                         or (:columnname = 'ACCOUNT_MANAGER_ORG_UNIT'
                                             and exists (select 1 from CustomerAccountManager cam2, AccountManager am
                                             where cam2.customerDetail.id = cd.id
                                             and cam2.managerId = am.id
                                             and cam2.status = 'ACTIVE'
                                             and lower(am.organizationalUnit) like :columnvalue))
                                         or (:columnname = 'ACCOUNT_MANAGER_BUS_UNIT'
                                             and exists (select 1 from CustomerAccountManager cam2, AccountManager am
                                             where cam2.customerDetail.id = cd.id
                                             and cam2.managerId = am.id
                                             and cam2.status = 'ACTIVE'
                                             and lower(am.businessUnit) like :columnvalue))
                                         or (:columnname = 'ACCOUNT_MANAGER_DISPLAY_NAME'
                                             and exists (select 1 from CustomerAccountManager cam2, AccountManager am
                                             where cam2.customerDetail.id = cd.id
                                             and cam2.managerId = am.id
                                             and cam2.status = 'ACTIVE'
                                             and lower(am.displayName) like :columnvalue))
                                     )
                                 )
                         )
                    """
    )
    Page<CustomerListingResponse> filter(
            @Param("columnvalue") String prompt,
            @Param("columnname") String searchField,
            @Param("customertype") List<CustomerType> customerFilterType,
            @Param("status") List<CustomerDetailStatus> status,
            @Param("economicbranch") List<Long> economicBranchCiIds,
            @Param("isunwantedcustomer") String unwantedCustomerListingStatus,
            @Param("customeraccountmanager") List<Long> customerAccountManagerIds,
            @Param("populatedplace") String populatedPlace,
            @Param("displayNameDirection") String displayNameDirection,
            @Param("customerStatuses") List<CustomerStatus> customerStatuses,
            @Param("systemUserId") String systemUserId,
            @Param("canEditCustomer") String canEditCustomer,
            @Param("excludepastversion") String excludePastVersion,
            Pageable pageable
    );


    /**
     * <h1>FindByIdAndStatuses</h1>
     *
     * @param id
     * @param statuses
     * @return {@link Customer} object
     */
    @Query("""
                    select c from Customer c
                    where c.id = :id
                    and c.status in (:statuses)
            """)
    Optional<Customer> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<CustomerStatus> statuses);

    @Query(
            value = """
                            select count(c.id) > 0 from Customer c
                            left join CustomerDetails cd on c.id = cd.customerId
                            left JOIN CustomerAccountManager cam ON cd.id = cam.customerDetail.id
                            left JOIN AccountManager am ON cam.managerId = am.id
                                where c.id = :id
                                and am.userName = :managerUsername
                    """
    )
    boolean existsByManager(
            @Param("id") Long id,
            @Param("managerUsername") String managerUsername
    );

    /**
     * <h1>GetCustomerByStatusAndCustomerNumber</h1>
     * query selects {@link Customer} table,
     * and returns data according to {@link CustomerStatus} and customerNumber
     *
     * @param status
     * @param customerNumber
     * @return {@link Customer} object
     */
    @Query(
            """
                            select c 
                            from Customer c
                             where c.status =:status 
                             and c.customerNumber =:customer_number
                    
                    """

    )
    Optional<Customer> getCustomerByStatusAndCustomerNumber(@Param("status") CustomerStatus status,
                                                            @Param("customer_number") Long customerNumber);


    /**
     * <h1>FindByIdentifierAndStatus</h1>
     * query selects {@link Customer} table,
     * and returns data according to customer identifier and customer status
     *
     * @param identifier
     * @param status
     * @return
     */
    @Query("""
                select c
                from Customer c
                where c.identifier = :identifier
                and c.status =:status
            """)
    Optional<Customer> findByIdentifierAndStatus(@Param("identifier") String identifier, @Param("status") CustomerStatus status);

    boolean existsByIdAndCustomerTypeInAndStatusIn(Long id, List<CustomerType> customerTypes, List<CustomerStatus> statuses);


    @Query(
            value = """
                    select count(c.id) > 0 from Customer c
                    join Discount d on d.customerId = c.id
                        where c.id = :id
                        and d.status = 'ACTIVE'
                        and c.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToDiscount(@Param("id") Long id);


    @Query(
            value = """
                    select count (c.id) > 0 from Customer c
                    join PointOfDeliveryDetails podd on podd.customerId = c.id
                    join PointOfDelivery pod on pod.id = podd.podId
                        where c.id = :id
                        and pod.status = 'ACTIVE'
                        and c.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToPointOfDelivery(@Param("id") Long id);

    boolean existsByIdAndStatusIn(Long customerId, List<CustomerStatus> statuses);


    @Query(
            value = """
                    select count (c.id) > 0 from Customer c
                    join CustomerConnectedGroup ccg on ccg.customerId = c.id
                    join ConnectedGroup cg on ccg.connectedGroupId = cg.id
                        where c.id = :id
                        and cg.status = 'ACTIVE'
                        and c.status = 'ACTIVE'
                        and ccg.status = 'ACTIVE'
                    """
    )
    boolean isInGroupOfConnectedCustomers(@Param("id") Long id);

    boolean existsByIdentifierAndStatusIn(String identifier, List<CustomerStatus> statuses);

    List<Customer> findAllByIdInAndStatusIn(List<Long> ids, List<CustomerStatus> statuses);


    @Query(
            value = """
                            select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                                cc.id,
                                cc.contactTypeName,
                                cc.createDate
                            )
                            from CustomerCommContactPurposes cccp
                            join ContactPurpose cp on cccp.contactPurposeId = cp.id
                            join CustomerCommunications cc on cc.id=cccp.customerCommunicationsId
                            where cc.customerDetailsId = :customerDetailsId
                            and cp.id = :purposeId
                            and cp.status = 'ACTIVE'
                            and cc.status = 'ACTIVE'
                            and cccp.status = 'ACTIVE'
                            and exists(
                                select 1 from CustomerCommunicationContacts ccc
                                    where ccc.customerCommunicationsId = cc.id
                                    and ccc.status = 'ACTIVE'
                                    and ccc.contactType = 'EMAIL'
                            )
                            and exists(
                                select 1 from CustomerCommunicationContacts ccc
                                    where ccc.customerCommunicationsId = cc.id
                                    and ccc.status = 'ACTIVE'
                                    and ccc.contactType = 'MOBILE_NUMBER'
                            )
                            order by cc.createDate desc
                    """
    )
    List<CustomerCommunicationDataResponse> customerCommunicationDataList(
            @Param("customerDetailsId") Long customerDetailsId,
            @Param("purposeId") Long purposeId
    );


    @Query(
            value = """
                    select count(c.id) > 0 from Customer c
                    left join CustomerActivity ca on ca.customerId = c.id
                    join SystemActivity sa on sa.id = ca.systemActivityId
                        where c.id = :id
                        and c.status = 'ACTIVE'
                        and ca.status = 'ACTIVE'
                        and sa.status = 'ACTIVE'
                    """
    )
    boolean hasConnectionToActivity(Long id);


    @Query(
            value = """
                    select c from Customer c
                    join CustomerDetails cd on c.id = cd.customerId
                        where cd.id = :customerDetailId
                        and c.status in (:statuses)
                    """
    )
    Optional<Customer> findByCustomerDetailIdAndStatusIn(Long customerDetailId, List<CustomerStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.action.ActionCustomerResponse(
                        c,
                        cd
                    )
                    from Customer c
                    join CustomerDetails cd on cd.id = c.lastCustomerDetailId
                        where c.status = 'ACTIVE'
                        and c.identifier = :prompt
                    """
    )
    ActionCustomerResponse findCustomerForAction(
            @Param("prompt") String prompt
    );

    @Query("""
            select count(pcd.id) > 0
            from Customer c
            join CustomerDetails cd on c.id = cd.customerId
            join ProductContractDetails pcd on cd.id = pcd.customerDetailId
            join ProductContract pc on pcd.contractId = pc.id
            where c.id = :id
            and pc.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToProductContract(Long id);

    @Query("""
            select count(scd.id) > 0
            from Customer c
            join CustomerDetails cd on c.id = cd.customerId
            join ServiceContractDetails scd on cd.id = scd.customerDetailId
            join ServiceContracts sc on scd.contractId = sc.id
            where c.id = :id
            and sc.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToServiceContract(Long id);

    @Query("""
            select count(so.id) > 0
            from Customer c
            join CustomerDetails cd on c.id = cd.customerId
            join ServiceOrder so on so.customerDetailId = cd.id
            where c.id = :id
            and so.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToServiceOrder(Long id);


    @Query("""
            select count(go.id) > 0
            from Customer c
            join CustomerDetails cd on c.id = cd.customerId
            join GoodsOrder go on go.customerDetailId = cd.id
            where c.id = :id
            and go.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToGoodsOrder(Long id);

    @Query("""
            select count(ct.id) > 0
            from Customer c
            join CustomerTask ct on c.id = ct.customerId
            where c.id = :id
            and ct.status = 'ACTIVE'
            """
    )
    boolean hasActiveConnectionToCustomerTask(Long id);

    @Query(
            value = """
                    select c.id from Customer c
                    join CustomerDetails cd on c.id = cd.customerId
                        where cd.id = :customerDetailId
                        and c.status in (:statuses)
                    """
    )
    Optional<Long> findCustomerIdByCustomerDetailIdAndStatusIn(Long customerDetailId, List<CustomerStatus> statuses);

    @Query(
            value = """
                            select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                                cc.id,
                                concat(cc.contactTypeName, ' (', cp.name, ')'),
                                cc.createDate
                            )
                            from CustomerCommContactPurposes cccp
                            join ContactPurpose cp on cccp.contactPurposeId = cp.id
                            join CustomerCommunications cc on cc.id=cccp.customerCommunicationsId
                            join CustomerDetails cd on cd.id = cc.customerDetailsId and cc.customerDetailsId = :customerDetailId
                            where cp.id = :purposeId
                            and cp.status = 'ACTIVE'
                            and cc.status = 'ACTIVE'
                            and cccp.status = 'ACTIVE'
                            and exists(
                                select 1 from CustomerCommunicationContacts ccc
                                    where ccc.customerCommunicationsId = cc.id
                                    and ccc.status = 'ACTIVE'
                                    and ccc.contactType = 'EMAIL'
                            )
                            and exists(
                                select 1 from CustomerCommunicationContacts ccc
                                    where ccc.customerCommunicationsId = cc.id
                                    and ccc.status = 'ACTIVE'
                                    and ccc.contactType = 'MOBILE_NUMBER'
                            )
                            order by cc.createDate desc
                    """
    )
    List<CustomerCommunicationDataResponse> customerCommunicationDataListForManualOffsetting(
            @Param("customerDetailId") Long customerDetailId,
            @Param("purposeId") Long purposeId
    );

    @Query(
            value = """
                    select * from
                    (SELECT STRING_TO_TABLE(:identifier, ',') as customer)
                    as tbl
                    where tbl.customer not in (select identifier from customer.customers) 
                    """,
            nativeQuery = true
    )
    List<String> findByStringIdentifierInAndStatus(@Param("identifier") String identifier);

    @Query("""
            select new bg.energo.phoenix.model.CacheObject(c.id,c.identifier)
            from Customer c
            where c.identifier=:identifier
            and c.status=:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findCacheObjectByName(String identifier, CustomerStatus status);

    @Query("""
            select new bg.energo.phoenix.model.CacheObject(c.id,c.identifier)
            from Customer c
            where c.customerNumber=:customerNumber
            and c.status=:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findCacheObjectByCustomerNumber(Long customerNumber, CustomerStatus status);

    @Query("""  
                    select new bg.energo.phoenix.model.CacheObject(c.id,c.identifier)
                    from Customer c
                    where c.identifier='000000000'
                    and c.isHardCoded = true
                    and c.status='ACTIVE'
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findHardcodedCustomer();

    @Transactional
    @Query(value = """
                WITH inserted_customer AS (
                    INSERT INTO customer.customers (
                                                    identifier, is_hard_coded, status, customer_type, customer_number, system_user_id
                        ) VALUES ('000000000', true, 'ACTIVE', 'LEGAL_ENTITY', :customerNumber, 'system')
                        RETURNING id
                ),
                     inserted_details AS (
                         INSERT INTO customer.customer_details (
                                                                name, name_transl, customer_id, version_id, public_procurement_law,
                                                                foreign_entity_person, foreign_address, system_user_id,
                                                                create_date, status
                             )
                             SELECT 'User', 'User', id, 1, false, false, false,
                                    'system', now(), 'ACTIVE'
                             FROM inserted_customer
                             RETURNING id, customer_id
                     )
                SELECT id FROM inserted_customer
            """, nativeQuery = true)
    Long insertHardCodedCustomer(@Param("customerNumber") Long customerNumber);

    @Transactional
    @Modifying
    @Query(value = """
                WITH updated_customers AS (
                    UPDATE customer.customers c
                        SET last_customer_detail_id = cd.id
                        FROM customer.customer_details cd
                        WHERE cd.customer_id = :customerId
                            AND c.id = :customerId
                        RETURNING cd.id AS detail_id
                )
                UPDATE customer.customer_details cd
                SET legal_form_id = lf.id
                FROM nomenclature.legal_forms lf, updated_customers uc
                WHERE cd.id = uc.detail_id
                  AND lf.is_default = true
            """, nativeQuery = true)
    void setDetailIdToHardcodedCustomer(Long customerId);

    @Query("""
            select c.identifier
            from Customer c
            where c.identifier in :identifier
            """)
    Set<String> findIdentifiersByIdentifierInAndStatusIn(
            @Param("identifier") Collection<String> identifier
    );

    @Query("""
            select new bg.energo.phoenix.model.CacheObjectForCustomer(c.id,c.identifier,c.customerNumber)
            from Customer c
            where c.identifier=:identifier
            and c.status=:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObjectForCustomer> findCacheObjectCustomerNumber(String identifier, CustomerStatus status);

    @Query("""
                select cd.versionId
                from CustomerDetails cd
                join Customer customer on cd.id = customer.lastCustomerDetailId
                where customer.identifier = :identifier
                and customer.status =:status
            """)
    Long findLastVersionIdByIdentifierAndStatus(@Param("identifier") String identifier, @Param("status") CustomerStatus status);

    @Query(nativeQuery = true,
            value = """
                    select string_agg(nopur.name, ', ')  from customer.customer_comm_contact_purposes pur
                    join nomenclature.contact_purposes nopur
                    on pur.contact_purpose_id = nopur.id
                    where customer_communication_id = :customerCommunicationId
                    and pur.status = 'ACTIVE'
                    """
    )
    String getConcatPurposeFromCustomerCommunicationData(Long customerCommunicationId);

    @Query(value = """
                    select string_agg(nopur.name, ', ')  from customer.customer_comm_contact_purposes pur
                                join nomenclature.contact_purposes nopur
                                on pur.contact_purpose_id = nopur.id
                                where customer_communication_id = :customerCommunicationId
                                and pur.status = 'ACTIVE'
                                and pur.contact_purpose_id in :purposeIds
            """, nativeQuery = true)
    String getContactPurposeFromCustomerCommunicationDataAndPurposeIds(Long customerCommunicationId, Set<Long> purposeIds);

    @Query(nativeQuery = true,
            value = """
                    select string_agg(nopur.name, ', ') as purposes,cc.id as communicationId  from customer.customer_communications cc
                    left join  customer.customer_comm_contact_purposes pur
                    on pur.customer_communication_id=cc.id
                    join nomenclature.contact_purposes nopur
                    on pur.contact_purpose_id = nopur.id
                    where cc.id in :customerCommunicationIds
                    and pur.status = 'ACTIVE'
                    group by communicationId
                    """
    )
    List<ContactPurposeMiddleResponse> getConcatPurposeFromCustomerCommunicationData(List<Long> customerCommunicationIds);

    @Query(value = """
             WITH service_contract_details AS (SELECT cd.id                                                          AS customerDetailId,
                                                      c.identifier                                                   AS customerIdentifier,
                                                      cd.version_id                                                  AS customerVersion,
                                                      ARRAY_AGG(DISTINCT sccd.id) FILTER (WHERE sccd.id IS NOT NULL) AS serviceContractDetailIds
                                               FROM service_contract.contract_details sccd
                                                        JOIN service_contract.contracts pcc ON sccd.contract_id = pcc.id
                                                        JOIN customer.customer_details cd ON cd.id = sccd.customer_detail_id
                                                        JOIN customer.customers c ON c.id = cd.customer_id
                                               WHERE pcc.contract_status IN ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                                                 AND pcc.status = 'ACTIVE'
                                                 AND c.status = 'ACTIVE'
                                                 AND (sccd.start_date <= CURRENT_DATE AND sccd.end_date IS NULL
                                                   OR sccd.start_date <= CURRENT_DATE AND sccd.end_date >= CURRENT_DATE)
                                               GROUP BY cd.id, c.identifier, cd.version_id),
                  product_contract_details AS (SELECT cd.id                                                          AS customerDetailId,
                                                      c.identifier                                                   AS customerIdentifier,
                                                      cd.version_id                                                  AS customerVersion,
                                                      ARRAY_AGG(DISTINCT pccd.id) FILTER (WHERE pccd.id IS NOT NULL) AS productContractDetailIds
                                               FROM product_contract.contract_details pccd
                                                        JOIN product_contract.contracts pcc ON pccd.contract_id = pcc.id
                                                        JOIN customer.customer_details cd ON cd.id = pccd.customer_detail_id
                                                        JOIN customer.customers c ON c.id = cd.customer_id
                                               WHERE pcc.contract_status IN ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                                                 AND pcc.status = 'ACTIVE'
                                                 AND c.status = 'ACTIVE'
                                                 AND pccd.status = 'SIGNED'
                                                 AND (pccd.start_date <= CURRENT_DATE AND pccd.end_date IS NULL
                                                   OR pccd.start_date <= CURRENT_DATE AND pccd.end_date >= CURRENT_DATE)
                                               GROUP BY cd.id, c.identifier, cd.version_id)
             SELECT COALESCE(sc.customerDetailId, pc.customerDetailId)     AS customerDetailId,
                    COALESCE(sc.customerIdentifier, pc.customerIdentifier) AS customerIdentifier,
                    COALESCE(sc.customerVersion, pc.customerVersion)       AS customerVersion,
                    sc.serviceContractDetailIds                            AS serviceContractDetailIds,
                    pc.productContractDetailIds                            AS productContractDetailIds
             FROM service_contract_details sc
                      FULL OUTER JOIN product_contract_details pc
                                      ON sc.customerDetailId = pc.customerDetailId
                                          AND sc.customerIdentifier = pc.customerIdentifier
                                          AND sc.customerVersion = pc.customerVersion
             GROUP BY COALESCE(sc.customerDetailId, pc.customerDetailId),
                      COALESCE(sc.customerIdentifier, pc.customerIdentifier),
                      COALESCE(sc.customerVersion, pc.customerVersion),
                      sc.serviceContractDetailIds,
                      pc.productContractDetailIds
            """,
            nativeQuery = true
    )
    List<ActiveContractsAndAssociatedCustomersForMassCommunicationProjection> fetchActiveContractsAndAssociatedCustomersForMassCommunication();

    @Query(value = """
            select c.identifier
            from Customer c
            join CustomerDetails cd on cd.customerId = c.id
            where cd.id = :customerDetailId
            """)
    String getCustomerIdentifierByCustomerDetailId(@Param("customerDetailId") Long customerDetailId);


    @Query(nativeQuery = true,
            value = """
                    Select bool_or(e.disconnected)
                    from customer.customers f
                             left join customer.customer_details g on f.id = g.customer_id
                             left join product_contract.contract_details a on a.customer_detail_id = g.id
                             left join product_contract.contract_pods c on c.contract_detail_id = a.id
                             left join pod.pod_details d on c.pod_detail_id = d.id
                             inner join pod.pod e on d.pod_id = e.id
                    where f.id = :customerId
                    group by f.id
                    """)
    Boolean checkCustomerPodDisconnectionStatus(@Param("customerId") Long customerId);


    @Query(value = """
                select rel.massOrIndCommunicationId,
                       rel.LinkedCommunicationId,
                       rel.activity,
                       rel.creatorEmployee,
                       rel.senderEmployee,
                       rel.contactPurpose,
                       rel.communicationType,
                       rel.communicationData,
                       rel.sentReceiveDate,
                       rel.createDate,
                       rel.communicationTopic,
                       rel.communicationStatus,
                       rel.status,
                       rel.communicationChannel,
                       rel.relationType,
                       rel.subject
                from(
                        select email.massOrIndCommunicationId,
                               email.LinkedCommunicationId,
                               email.activity,
                               email.creatorEmployee,
                               email.senderEmployee,
                               email.contactPurpose,
                               email.communicationType,
                               email.communicationData,
                               email.sentReceiveDate,
                               email.createDate,
                               email.communicationTopic,
                               email.communicationStatus,
                               email.status,
                               email.communicationChannel,
                               email.relationType,
                               email.subject
                        from(
                                select distinct
                                    ec.id as massOrIndCommunicationId,
                                    cast(null as integer) as LinkedCommunicationId,
                                    case when lower(text(:activityDirection)) = 'desc'
                                             then
                                             veca.activityDesc
                                         else
                                             veca.activityAsc
                                        end as activity,
                                    am.display_name as creatorEmployee,
                                    am2.display_name as senderEmployee,
                                    case when lower(text(:contactPurposeDirection)) = 'desc'
                                             then
                                             veccp.contactPurposeDesc
                                         else
                                             veccp.contactPurposeAsc
                                        end as contactPurpose,
                                    text(ec.communication_type) as communicationType,
                                    null as communicationData,
                                    ec.sent_date as sentReceiveDate,
                                    ec.create_date as createDate,
                                    ct.name as communicationTopic,
                                    text(ec.communication_status) as communicationStatus,
                                    text(ec.status) as status,
                                    'MASS_EMAIL' as communicationChannel,
                                    'EMAIL' as relationtype,
                                    null as subject
                                from
                                    crm.email_communications ec
                                        left join crm.vw_email_communication_contact_purposes veccp on veccp.email_communication_id = ec.id
                                        left join crm.vw_email_communication_activity veca on veca.email_communication_id = ec.id
                                        join
                                    crm.email_communication_customers ecc
                                    on ecc.email_communication_id = ec.id
                                        left join
                                    crm.email_communication_customer_contacts eccc
                                    on
                                        eccc.email_communication_customer_id = ecc.id
                                        join customer.customer_communications cc
                                             on ecc.customer_communication_id = cc.id
                                                 and ecc.customer_detail_id = :customerDetailId
                                        join
                                    nomenclature.communication_topics ct
                                    on ec.communication_topic_id = ct.id
                                        join
                                    customer.customer_details cd
                                    on ecc.customer_detail_id = cd.id
                                        join
                                    customer.customers c
                                    on cd.customer_id = c.id
                                        left join
                                    nomenclature.legal_forms lf
                                    on cd.legal_form_id = lf.id
                                        join
                                    customer.account_managers am
                                    on ec.system_user_id = am.user_name
                                        left join
                                    customer.account_managers am2
                                    on ec.sender_employee_id = am2.id
                                        left join
                                    crm.email_communication_contact_purposes sccp
                                    on sccp.email_communication_id = ec.id
                                        and sccp.status = 'ACTIVE'
                                        left join
                                    nomenclature.contact_purposes cp
                                    on sccp.contact_purpose_id = cp.id
                                        left join
                                    crm.email_communication_activity sca
                                    on sca.email_communication_id= ec.id
                                        and sca.status = 'ACTIVE'
                                        left join
                                    activity.activity act
                                    on sca.activity_id = act.id
                                        left join
                                    crm.email_communication_tasks sct
                                    on sct.email_communication_id = ec.id
                                        and sct.status = 'ACTIVE'
                                where ec.communication_channel = 'MASS_EMAIL' and
                                    ((:entityStatuses) is null or text(ec.status)  in :entityStatuses)
                                  and
                                    ((:creatorEmployeeId) is null or am.id  in :creatorEmployeeId)
                                  and
                                    ((:senderEmployeeId) is null or am2.id  in :senderEmployeeId)
                                  and
                                    (date(:createDateFrom) is null or date(ec.create_date) >= date(:createDateFrom))
                                  and
                                    (date(:createDateTo) is null or date(ec.create_date) <= date(:createDateTo))
                                  and
                                    ((:contactPurposeId) is null or cp.id in :contactPurposeId)
                                  and
                                    ((:communicationType) is null or text(ec.communication_type)  in :communicationType)
                                  and
                                    ((:activityId) is null or act.activity_id in :activityId)
                                  and
                                    ((:taskId) is null or sct.task_id in :taskId)
                                  and
                                    ((:communicationTopicId) is null or ec.communication_topic_id  in :communicationTopicId)
                                  and
                                    ((:communicationStatus) is null or text(ec.communication_status)  in :communicationStatus)
                                  and (:prompt is null or (:searchBy = 'ALL' and (
                                    text(ecc.id) like :prompt
                                        or
                                    text(ec.id) like :prompt
                                        or
                                    lower(text(cc.contact_type_name)) like :prompt
                                        or
                                    lower(text(cd.name)) like :prompt
                                        or
                                    lower(text(c.identifier)) like :prompt
                                        or
                                    lower(text(c.customer_number)) like :prompt
                                        or
                                    lower(text(eccc.email_address)) like :prompt
                                        or
                                    lower(ec.dms_number) like :prompt
                                        or
                                    lower(ec.email_subject) like :prompt
                                    )
                                    )
                                    or (
                                           (:searchBy = 'COMMUNICATION_ID' and text(ecc.id) like :prompt)
                                               or
                                           (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and text(ec.id) like :prompt)
                                               or
                                           (:searchBy = 'COMMUNICATION_DATA' and lower(text(cc.contact_type_name)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_NAME' and lower(text(cd.name)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                                               or
                                           (:searchBy = 'EMAIL_ADDRESS' and lower(text(eccc.email_address)) like :prompt)
                                               or
                                           (:searchBy = 'DMS_NUMBER' and lower(ec.dms_number) like :prompt)
                                               or
                                           (:searchBy = 'SUBJECT' and lower(ec.email_subject) like :prompt)
                                           ))
                                union all
                                select distinct
                                    case when ec.communication_channel =  'MASS_EMAIL' then ecc.id else ec.id end,
                                    case when ec.communication_channel =  'MASS_EMAIL' then ec.id else null end,
                                    case when lower(text(:activityDirection)) = 'desc'
                                             then
                                             veca.activityDesc
                                         else
                                             veca.activityAsc
                                        end as activity,
                                    am.display_name as creatorEmployee,
                                    am2.display_name as senderEmployee,
                                    case when lower(text(:contactPurposeDirection)) = 'desc'
                                             then
                                             veccp.contactPurposeDesc
                                         else
                                             veccp.contactPurposeAsc
                                        end as contactPurpose,
                                    text(ec.communication_type) as communicationType,
                                    cc.contact_type_name as communicationData,
                                    ec.sent_date as sentReceiveDate,
                                    ec.create_date as createDate,
                                    ct.name as communicationTopic,
                                    coalesce(text(ec.communication_status),text(ecc.status)) as communicationStatus,
                                    text(ec.status) as status,
                                    'EMAIL' as communicationChannel,
                                    'EMAIL',
                                    ec.email_subject as subject
                                from
                                    crm.email_communications ec
                                        left join crm.vw_email_communication_contact_purposes veccp on veccp.email_communication_id = ec.id
                                        left join crm.vw_email_communication_activity veca on veca.email_communication_id = ec.id
                                        join
                                    crm.email_communication_customers ecc
                                    on ecc.email_communication_id = ec.id
                                        and ecc.customer_detail_id = :customerDetailId
                                        left join
                                    crm.email_communication_customer_contacts eccc
                                    on
                                        eccc.email_communication_customer_id = ecc.id
                                        join customer.customer_communications cc
                                             on ecc.customer_communication_id = cc.id
                                        join
                                    nomenclature.communication_topics ct
                                    on ec.communication_topic_id = ct.id
                                        join
                                    customer.customer_details cd
                                    on ecc.customer_detail_id = cd.id
                                        join
                                    customer.customers c
                                    on cd.customer_id = c.id
                                        left join
                                    nomenclature.legal_forms lf
                                    on cd.legal_form_id = lf.id
                                        join
                                    customer.account_managers am
                                    on ec.system_user_id = am.user_name
                                        left join
                                    customer.account_managers am2
                                    on ec.sender_employee_id = am2.id
                                        left join
                                    crm.email_communication_contact_purposes sccp
                                    on sccp.email_communication_id = ec.id
                                        and sccp.status = 'ACTIVE'
                                        left join
                                    nomenclature.contact_purposes cp
                                    on sccp.contact_purpose_id = cp.id
                                        left join
                                    crm.email_communication_activity sca
                                    on sca.email_communication_id= ec.id
                                        and sca.status = 'ACTIVE'
                                        left join
                                    activity.activity act
                                    on sca.activity_id = act.id
                                        left join
                                    crm.email_communication_tasks sct
                                    on sct.email_communication_id = ec.id
                                        and sct.status = 'ACTIVE'
                                where
                                    ((:entityStatuses) is null or text(ec.status)  in :entityStatuses)
                                  and
                                    ((:creatorEmployeeId) is null or am.id  in :creatorEmployeeId)
                                  and
                                    ((:senderEmployeeId) is null or am2.id  in :senderEmployeeId)
                                  and
                                    (date(:createDateFrom) is null or date(ec.create_date) >= date(:createDateFrom))
                                  and
                                    (date(:createDateTo) is null or date(ec.create_date) <= date(:createDateTo))
                                  and
                                    ((:contactPurposeId) is null or cp.id in :contactPurposeId)
                                  and
                                    ((:communicationType) is null or text(ec.communication_type)  in :communicationType)
                                  and
                                    ((:activityId) is null or act.activity_id in :activityId)
                                  and
                                    ((:taskId) is null or sct.task_id in :taskId)
                                  and
                                    ((:communicationTopicId) is null or ec.communication_topic_id  in :communicationTopicId)
                                  and
                                    ((:communicationStatus) is null or coalesce(text(ec.communication_status),text(ecc.status))  in :communicationStatus)
                                  and (:prompt is null or (:searchBy = 'ALL' and (
                                    text(ecc.id) like :prompt
                                        or
                                    text(ec.id) like :prompt
                                        or
                                    lower(text(cc.contact_type_name)) like :prompt
                                        or
                                    lower(text(cd.name)) like :prompt
                                        or
                                    lower(text(c.identifier)) like :prompt
                                        or
                                    lower(text(c.customer_number)) like :prompt
                                        or
                                    lower(text(eccc.email_address)) like :prompt
                                        or
                                    lower(ec.dms_number) like :prompt
                                        or
                                    lower(ec.email_subject) like :prompt
                                    )
                                    )
                                    or (
                                           (:searchBy = 'COMMUNICATION_ID' and text(ecc.id) = :prompt)
                                               or
                                           (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and ec.communication_channel = 'MASS_EMAIL' and text(ecc.email_communication_id) = :prompt)
                                               or
                                           (:searchBy = 'COMMUNICATION_DATA' and lower(text(cc.contact_type_name)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_NAME' and lower(text(cd.name)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                                               or
                                           (:searchBy = 'EMAIL_ADDRESS' and lower(text(eccc.email_address)) like :prompt)
                                               or
                                           (:searchBy = 'DMS_NUMBER' and lower(ec.dms_number) like :prompt)
                                               or
                                           (:searchBy = 'SUBJECT' and lower(ec.email_subject) like :prompt)
                                           ))
                            ) as email
                        union all
                        select sms.massOrIndCommunicationId,
                               sms.LinkedCommunicationId,
                               sms.activity,
                               sms.creatorEmployee,
                               sms.senderEmployee,
                               sms.contactPurpose,
                               sms.communicationType,
                               sms.communicationData,
                               sms.sentReceiveDate,
                               sms.createDate,
                               sms.communicationTopic,
                               sms.communicationStatus,
                               sms.status,
                               sms.communicationChannel,
                               sms.relationType,
                               sms.subject
                        from(
                                select distinct
                                    sc.id as massOrIndCommunicationId,
                                    cast(null as integer) as LinkedCommunicationId,
                                    case when lower(text(:activityDirection)) = 'desc'
                                             then
                                             vsca.activityDesc
                                         else
                                             vsca.activityAsc
                                        end as activity,
                                    am.display_name as creatorEmployee,
                                    am2.display_name as senderEmployee,
                                    case when lower(text(:contactPurposeDirection)) = 'desc'
                                             then
                                             vsccp.contactPurposeDesc
                                         else
                                             vsccp.contactPurposeAsc
                                        end as contactPurpose,
                                    text(sc.communication_type) as communicationType,
                                    null as communicationData,
                                    sc.sent_date as sentReceiveDate,
                                    sc.create_date as createDate,
                                    ct.name as communicationTopic,
                                    text(sc.communication_status) as communicationStatus,
                                    text(sc.status) as status,
                                    'MASS_SMS' as communicationChannel,
                                    'SMS' as relationType,
                                    null as subject
                                from
                                    crm.sms_communications sc
                                        left join crm.vw_sms_communication_contact_purposes vsccp on vsccp.sms_communication_id = sc.id
                                        left join crm.vw_sms_communication_activity vsca on vsca.sms_communication_id = sc.id
                                        join
                                    crm.sms_communication_customers scc
                                    on scc.sms_communication_id = sc.id
                                        and scc.customer_detail_id = :customerDetailId
                                        left join
                                    crm.sms_communication_customer_contacts sccc
                                    on
                                        sccc.sms_communication_customer_id = scc.id
                                        join customer.customer_communications cc
                                             on scc.customer_communication_id = cc.id
                                        join
                                    nomenclature.communication_topics ct
                                    on sc.communication_topic_id = ct.id
                                        join
                                    customer.customer_details cd
                                    on scc.customer_detail_id = cd.id
                                        join
                                    customer.customers c
                                    on cd.customer_id = c.id
                                        left join
                                    nomenclature.legal_forms lf
                                    on cd.legal_form_id = lf.id
                                        join
                                    customer.account_managers am
                                    on sc.system_user_id = am.user_name
                                        left join
                                    customer.account_managers am2
                                    on sc.sender_employee_id = am2.id
                                        left join
                                    crm.sms_communication_contact_purposes sccp
                                    on sccp.sms_communication_id = sc.id
                                        and sccp.status = 'ACTIVE'
                                        left join
                                    nomenclature.contact_purposes cp
                                    on sccp.contact_purpose_id = cp.id
                                        left join
                                    crm.sms_communication_activity sca
                                    on sca.sms_communication_id= sc.id
                                        and sca.status = 'ACTIVE'
                                        left join
                                    activity.activity act
                                    on sca.activity_id = act.id
                                        left join
                                    crm.sms_communication_tasks sct
                                    on sct.sms_communication_id = sc.id
                                        and sct.status = 'ACTIVE'
                                where sc.communication_channel = 'MASS_SMS' and
                                    ((:entityStatuses) is null or text(sc.status)  in :entityStatuses)
                                  and
                                    ((:creatorEmployeeId) is null or am.id  in :creatorEmployeeId)
                                  and
                                    ((:senderEmployeeId) is null or am2.id  in :senderEmployeeId)
                                  and
                                    (date(:createDateFrom) is null or date(sc.create_date) >= date(:createDateFrom))
                                  and
                                    (date(:createDateTo) is null or date(sc.create_date) <= date(:createDateTo))
                                  and
                                    ((:contactPurposeId) is null or cp.id in :contactPurposeId)
                                  and
                                    ((:communicationType) is null or text(sc.communication_type)  in :communicationType)
                                  and
                                    ((:activityId) is null or act.activity_id in :activityId)
                                  and
                                    ((:taskId) is null or sct.task_id in :taskId)
                                  and
                                    ((:communicationTopicId) is null or sc.communication_topic_id  in :communicationTopicId)
                                  and
                                    ((:communicationStatus) is null or text(sc.communication_status)  in :communicationStatus)
                                  and (:prompt is null or (:searchBy = 'ALL' and (
                                    text(scc.id) like :prompt
                                        or
                                    text(sc.id) like :prompt
                                        or
                                    lower(text(cc.contact_type_name)) like :prompt
                                        or
                                    lower(text(cd.name)) like :prompt
                                        or
                                    lower(text(c.identifier)) like :prompt
                                        or
                                    lower(text(c.customer_number)) like :prompt
                                        or
                                    lower(text(sccc.phone_number)) like :prompt
                                    )
                                    )
                                    or (
                                           (:searchBy = 'COMMUNICATION_ID' and text(scc.id) like :prompt)
                                               or
                                           (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and text(sc.id) like :prompt)
                                               or
                                           (:searchBy = 'COMMUNICATION_DATA' and lower(text(cc.contact_type_name)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_NAME' and lower(text(cd.name)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                                               or
                                           (:searchBy = 'PHONE_NUMBER' and lower(text(sccc.phone_number)) like :prompt)
                                           ))
                                union all
                                select distinct
                                    scc.id,
                                    case when sc.communication_channel =  'MASS_SMS' then sc.id else null end,
                                    case when lower(text(:activityDirection)) = 'desc'
                                             then
                                             vsca.activityDesc
                                         else
                                             vsca.activityAsc
                                        end as activity,
                                    am.display_name as creatorEmployee,
                                    am2.display_name as senderEmployee,
                                    case when lower(text(:contactPurposeDirection)) = 'desc'
                                             then
                                             vsccp.contactPurposeDesc
                                         else
                                             vsccp.contactPurposeAsc
                                        end as contactPurpose,
                                    text(sc.communication_type) as communicationType,
                                    cc.contact_type_name as communicationData,
                                    sc.sent_date as sentReceiveDate,
                                    sc.create_date as createDate,
                                    ct.name as communicationTopic,
                                    text(coalesce(sc.communication_status,scc.sms_comm_status)) as communicationStatus,
                                    text(sc.status) as status,
                                    'SMS' as communicationChannel,
                                    'SMS',
                                    null
                                from
                                    crm.sms_communications sc
                                        left join crm.vw_sms_communication_contact_purposes vsccp on vsccp.sms_communication_id = sc.id
                                        left join crm.vw_sms_communication_activity vsca on vsca.sms_communication_id = sc.id
                                        join
                                    crm.sms_communication_customers scc
                                    on scc.sms_communication_id = sc.id
                                        and scc.customer_detail_id = :customerDetailId
                                        left join
                                    crm.sms_communication_customer_contacts sccc
                                    on
                                        sccc.sms_communication_customer_id = scc.id
                                        join customer.customer_communications cc
                                             on scc.customer_communication_id = cc.id
                                        join
                                    nomenclature.communication_topics ct
                                    on sc.communication_topic_id = ct.id
                                        join
                                    customer.customer_details cd
                                    on scc.customer_detail_id = cd.id
                                        join
                                    customer.customers c
                                    on cd.customer_id = c.id
                                        left join
                                    nomenclature.legal_forms lf
                                    on cd.legal_form_id = lf.id
                                        join
                                    customer.account_managers am
                                    on sc.system_user_id = am.user_name
                                        left join
                                    customer.account_managers am2
                                    on sc.sender_employee_id = am2.id
                                        left join
                                    crm.sms_communication_contact_purposes sccp
                                    on sccp.sms_communication_id = sc.id
                                        and sccp.status = 'ACTIVE'
                                        left join
                                    nomenclature.contact_purposes cp
                                    on sccp.contact_purpose_id = cp.id
                                        left join
                                    crm.sms_communication_activity sca
                                    on sca.sms_communication_id= sc.id
                                        and sca.status = 'ACTIVE'
                                        left join
                                    activity.activity act
                                    on sca.activity_id = act.id
                                        left join
                                    crm.sms_communication_tasks sct
                                    on sct.sms_communication_id = sc.id
                                        and sct.status = 'ACTIVE'
                                where
                                    ((:entityStatuses) is null or text(sc.status)  in :entityStatuses)
                                  and
                                    ((:creatorEmployeeId) is null or am.id  in :creatorEmployeeId)
                                  and
                                    ((:senderEmployeeId) is null or am2.id  in :senderEmployeeId)
                                  and
                                    (date(:createDateFrom) is null or date(sc.create_date) >= date(:createDateFrom))
                                  and
                                    (date(:createDateTo) is null or date(sc.create_date) <= date(:createDateTo))
                                  and
                                    ((:contactPurposeId) is null or cp.id in :contactPurposeId)
                                  and
                                    ((:communicationType) is null or text(sc.communication_type)  in :communicationType)
                                  and
                                    ((:activityId) is null or act.activity_id in :activityId)
                                  and
                                    ((:taskId) is null or sct.task_id in :taskId)
                                  and
                                    ((:communicationTopicId) is null or sc.communication_topic_id  in :communicationTopicId)
                                  and
                                    ((:communicationStatus) is null or text(coalesce(sc.communication_status,scc.sms_comm_status))  in :communicationStatus)
                                  and (:prompt is null or (:searchBy = 'ALL' and (
                                    text(scc.id) like :prompt
                                        or
                                    text(sc.id) like :prompt
                                        or
                                    lower(text(cc.contact_type_name)) like :prompt
                                        or
                                    lower(text(cd.name)) like :prompt
                                        or
                                    lower(text(c.identifier)) like :prompt
                                        or
                                    lower(text(c.customer_number)) like :prompt
                                        or
                                    lower(text(sccc.phone_number)) like :prompt
                                    )
                                    )
                                    or (
                                           (:searchBy = 'COMMUNICATION_ID' and text(scc.id) = :prompt)
                                               or
                                           (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and sc.communication_channel = 'MASS_SMS' and text(scc.sms_communication_id) = :prompt)
                                               or
                                           (:searchBy = 'COMMUNICATION_DATA' and lower(text(cc.contact_type_name)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_NAME' and lower(text(cd.name)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                               or
                                           (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                                               or
                                           (:searchBy = 'PHONE_NUMBER' and lower(text(sccc.phone_number)) like :prompt)
                                           ))
                            ) as sms) as rel
                where
                    ((:kindOfCommunication) is null or text(rel.communicationChannel)  in (:kindOfCommunication))
                  and
                    ((:relationType) is null or text(rel.relationType)  in (:relationType))
            
            """, nativeQuery = true, countQuery = """
                   select 
                   COUNT(rel.massOrIndCommunicationId)
                       from(
                               select email.massOrIndCommunicationId,
                                      email.LinkedCommunicationId,
                                      email.activity,
                                      email.creatorEmployee,
                                      email.senderEmployee,
                                      email.contactPurpose,
                                      email.communicationType,
                                      email.communicationData,
                                      email.sentReceiveDate,
                                      email.createDate,
                                      email.communicationTopic,
                                      email.communicationStatus,
                                      email.status,
                                      email.communicationChannel,
                                      email.relationType,
                                      email.subject
                               from(
                                       select distinct
                                           ec.id as massOrIndCommunicationId,
                                           cast(null as integer) as LinkedCommunicationId,
                                           case when lower(text(:activityDirection)) = 'desc'
                                                    then
                                                    veca.activityDesc
                                                else
                                                    veca.activityAsc
                                               end as activity,
                                           am.display_name as creatorEmployee,
                                           am2.display_name as senderEmployee,
                                           case when lower(text(:contactPurposeDirection)) = 'desc'
                                                    then
                                                    veccp.contactPurposeDesc
                                                else
                                                    veccp.contactPurposeAsc
                                               end as contactPurpose,
                                           text(ec.communication_type) as communicationType,
                                           null as communicationData,
                                           ec.sent_date as sentReceiveDate,
                                           ec.create_date as createDate,
                                           ct.name as communicationTopic,
                                           text(ec.communication_status) as communicationStatus,
                                           text(ec.status) as status,
                                           'MASS_EMAIL' as communicationChannel,
                                           'EMAIL' as relationtype,
                                           null as subject
                                       from
                                           crm.email_communications ec
                                               left join crm.vw_email_communication_contact_purposes veccp on veccp.email_communication_id = ec.id
                                               left join crm.vw_email_communication_activity veca on veca.email_communication_id = ec.id
                                               join
                                           crm.email_communication_customers ecc
                                           on ecc.email_communication_id = ec.id
                                               left join
                                           crm.email_communication_customer_contacts eccc
                                           on
                                               eccc.email_communication_customer_id = ecc.id
                                               join customer.customer_communications cc
                                                    on ecc.customer_communication_id = cc.id
                                                        and ecc.customer_detail_id = :customerDetailId
                                               join
                                           nomenclature.communication_topics ct
                                           on ec.communication_topic_id = ct.id
                                               join
                                           customer.customer_details cd
                                           on ecc.customer_detail_id = cd.id
                                               join
                                           customer.customers c
                                           on cd.customer_id = c.id
                                               left join
                                           nomenclature.legal_forms lf
                                           on cd.legal_form_id = lf.id
                                               join
                                           customer.account_managers am
                                           on ec.system_user_id = am.user_name
                                               left join
                                           customer.account_managers am2
                                           on ec.sender_employee_id = am2.id
                                               left join
                                           crm.email_communication_contact_purposes sccp
                                           on sccp.email_communication_id = ec.id
                                               and sccp.status = 'ACTIVE'
                                               left join
                                           nomenclature.contact_purposes cp
                                           on sccp.contact_purpose_id = cp.id
                                               left join
                                           crm.email_communication_activity sca
                                           on sca.email_communication_id= ec.id
                                               and sca.status = 'ACTIVE'
                                               left join
                                           activity.activity act
                                           on sca.activity_id = act.id
                                               left join
                                           crm.email_communication_tasks sct
                                           on sct.email_communication_id = ec.id
                                               and sct.status = 'ACTIVE'
                                       where ec.communication_channel = 'MASS_EMAIL' and
                                           ((:entityStatuses) is null or text(ec.status)  in :entityStatuses)
                                         and
                                           ((:creatorEmployeeId) is null or am.id  in :creatorEmployeeId)
                                         and
                                           ((:senderEmployeeId) is null or am2.id  in :senderEmployeeId)
                                         and
                                           (date(:createDateFrom) is null or date(ec.create_date) >= date(:createDateFrom))
                                         and
                                           (date(:createDateTo) is null or date(ec.create_date) <= date(:createDateTo))
                                         and
                                           ((:contactPurposeId) is null or cp.id in :contactPurposeId)
                                         and
                                           ((:communicationType) is null or text(ec.communication_type)  in :communicationType)
                                         and
                                           ((:activityId) is null or act.activity_id in :activityId)
                                         and
                                           ((:taskId) is null or sct.task_id in :taskId)
                                         and
                                           ((:communicationTopicId) is null or ec.communication_topic_id  in :communicationTopicId)
                                         and
                                           ((:communicationStatus) is null or text(ec.communication_status)  in :communicationStatus)
                                         and (:prompt is null or (:searchBy = 'ALL' and (
                                           text(ecc.id) like :prompt
                                               or
                                           text(ec.id) like :prompt
                                               or
                                           lower(text(cc.contact_type_name)) like :prompt
                                               or
                                           lower(text(cd.name)) like :prompt
                                               or
                                           lower(text(c.identifier)) like :prompt
                                               or
                                           lower(text(c.customer_number)) like :prompt
                                               or
                                           lower(text(eccc.email_address)) like :prompt
                                               or
                                           lower(ec.dms_number) like :prompt
                                               or
                                           lower(ec.email_subject) like :prompt
                                           )
                                           )
                                           or (
                                                  (:searchBy = 'COMMUNICATION_ID' and text(ecc.id) like :prompt)
                                                      or
                                                  (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and text(ec.id) like :prompt)
                                                      or
                                                  (:searchBy = 'COMMUNICATION_DATA' and lower(text(cc.contact_type_name)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_NAME' and lower(text(cd.name)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                                                      or
                                                  (:searchBy = 'EMAIL_ADDRESS' and lower(text(eccc.email_address)) like :prompt)
                                                      or
                                                  (:searchBy = 'DMS_NUMBER' and lower(ec.dms_number) like :prompt)
                                                      or
                                                  (:searchBy = 'SUBJECT' and lower(ec.email_subject) like :prompt)
                                                  ))
                                       union all
                                       select distinct
                                           case when ec.communication_channel =  'MASS_EMAIL' then ecc.id else ec.id end,
                                           case when ec.communication_channel =  'MASS_EMAIL' then ec.id else null end,
                                           case when lower(text(:activityDirection)) = 'desc'
                                                    then
                                                    veca.activityDesc
                                                else
                                                    veca.activityAsc
                                               end as activity,
                                           am.display_name as creatorEmployee,
                                           am2.display_name as senderEmployee,
                                           case when lower(text(:contactPurposeDirection)) = 'desc'
                                                    then
                                                    veccp.contactPurposeDesc
                                                else
                                                    veccp.contactPurposeAsc
                                               end as contactPurpose,
                                           text(ec.communication_type) as communicationType,
                                           cc.contact_type_name as communicationData,
                                           ec.sent_date as sentReceiveDate,
                                           ec.create_date as createDate,
                                           ct.name as communicationTopic,
                                           coalesce(text(ec.communication_status),text(ecc.status)) as communicationStatus,
                                           text(ec.status) as status,
                                           'EMAIL' as communicationChannel,
                                           'EMAIL',
                                           ec.email_subject as subject
                                       from
                                           crm.email_communications ec
                                               left join crm.vw_email_communication_contact_purposes veccp on veccp.email_communication_id = ec.id
                                               left join crm.vw_email_communication_activity veca on veca.email_communication_id = ec.id
                                               join
                                           crm.email_communication_customers ecc
                                           on ecc.email_communication_id = ec.id
                                               and ecc.customer_detail_id = :customerDetailId
                                               left join
                                           crm.email_communication_customer_contacts eccc
                                           on
                                               eccc.email_communication_customer_id = ecc.id
                                               join customer.customer_communications cc
                                                    on ecc.customer_communication_id = cc.id
                                               join
                                           nomenclature.communication_topics ct
                                           on ec.communication_topic_id = ct.id
                                               join
                                           customer.customer_details cd
                                           on ecc.customer_detail_id = cd.id
                                               join
                                           customer.customers c
                                           on cd.customer_id = c.id
                                               left join
                                           nomenclature.legal_forms lf
                                           on cd.legal_form_id = lf.id
                                               join
                                           customer.account_managers am
                                           on ec.system_user_id = am.user_name
                                               left join
                                           customer.account_managers am2
                                           on ec.sender_employee_id = am2.id
                                               left join
                                           crm.email_communication_contact_purposes sccp
                                           on sccp.email_communication_id = ec.id
                                               and sccp.status = 'ACTIVE'
                                               left join
                                           nomenclature.contact_purposes cp
                                           on sccp.contact_purpose_id = cp.id
                                               left join
                                           crm.email_communication_activity sca
                                           on sca.email_communication_id= ec.id
                                               and sca.status = 'ACTIVE'
                                               left join
                                           activity.activity act
                                           on sca.activity_id = act.id
                                               left join
                                           crm.email_communication_tasks sct
                                           on sct.email_communication_id = ec.id
                                               and sct.status = 'ACTIVE'
                                       where
                                           ((:entityStatuses) is null or text(ec.status)  in :entityStatuses)
                                         and
                                           ((:creatorEmployeeId) is null or am.id  in :creatorEmployeeId)
                                         and
                                           ((:senderEmployeeId) is null or am2.id  in :senderEmployeeId)
                                         and
                                           (date(:createDateFrom) is null or date(ec.create_date) >= date(:createDateFrom))
                                         and
                                           (date(:createDateTo) is null or date(ec.create_date) <= date(:createDateTo))
                                         and
                                           ((:contactPurposeId) is null or cp.id in :contactPurposeId)
                                         and
                                           ((:communicationType) is null or text(ec.communication_type)  in :communicationType)
                                         and
                                           ((:activityId) is null or act.activity_id in :activityId)
                                         and
                                           ((:taskId) is null or sct.task_id in :taskId)
                                         and
                                           ((:communicationTopicId) is null or ec.communication_topic_id  in :communicationTopicId)
                                         and
                                           ((:communicationStatus) is null or coalesce(text(ec.communication_status),text(ecc.status))  in :communicationStatus)
                                         and (:prompt is null or (:searchBy = 'ALL' and (
                                           text(ecc.id) like :prompt
                                               or
                                           text(ec.id) like :prompt
                                               or
                                           lower(text(cc.contact_type_name)) like :prompt
                                               or
                                           lower(text(cd.name)) like :prompt
                                               or
                                           lower(text(c.identifier)) like :prompt
                                               or
                                           lower(text(c.customer_number)) like :prompt
                                               or
                                           lower(text(eccc.email_address)) like :prompt
                                               or
                                           lower(ec.dms_number) like :prompt
                                               or
                                           lower(ec.email_subject) like :prompt
                                           )
                                           )
                                           or (
                                                  (:searchBy = 'COMMUNICATION_ID' and text(ecc.id) = :prompt)
                                                      or
                                                  (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and ec.communication_channel = 'MASS_EMAIL' and text(ecc.email_communication_id) = :prompt)
                                                      or
                                                  (:searchBy = 'COMMUNICATION_DATA' and lower(text(cc.contact_type_name)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_NAME' and lower(text(cd.name)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                                                      or
                                                  (:searchBy = 'EMAIL_ADDRESS' and lower(text(eccc.email_address)) like :prompt)
                                                      or
                                                  (:searchBy = 'DMS_NUMBER' and lower(ec.dms_number) like :prompt)
                                                      or
                                                  (:searchBy = 'SUBJECT' and lower(ec.email_subject) like :prompt)
                                                  ))
                                   ) as email
                               union all
                               select sms.massOrIndCommunicationId,
                                      sms.LinkedCommunicationId,
                                      sms.activity,
                                      sms.creatorEmployee,
                                      sms.senderEmployee,
                                      sms.contactPurpose,
                                      sms.communicationType,
                                      sms.communicationData,
                                      sms.sentReceiveDate,
                                      sms.createDate,
                                      sms.communicationTopic,
                                      sms.communicationStatus,
                                      sms.status,
                                      sms.communicationChannel,
                                      sms.relationType,
                                      sms.subject
                               from(
                                       select distinct
                                           sc.id as massOrIndCommunicationId,
                                           cast(null as integer) as LinkedCommunicationId,
                                           case when lower(text(:activityDirection)) = 'desc'
                                                    then
                                                    vsca.activityDesc
                                                else
                                                    vsca.activityAsc
                                               end as activity,
                                           am.display_name as creatorEmployee,
                                           am2.display_name as senderEmployee,
                                           case when lower(text(:contactPurposeDirection)) = 'desc'
                                                    then
                                                    vsccp.contactPurposeDesc
                                                else
                                                    vsccp.contactPurposeAsc
                                               end as contactPurpose,
                                           text(sc.communication_type) as communicationType,
                                           null as communicationData,
                                           sc.sent_date as sentReceiveDate,
                                           sc.create_date as createDate,
                                           ct.name as communicationTopic,
                                           text(sc.communication_status) as communicationStatus,
                                           text(sc.status) as status,
                                           'MASS_SMS' as communicationChannel,
                                           'SMS' as relationType,
                                           null as subject
                                       from
                                           crm.sms_communications sc
                                               left join crm.vw_sms_communication_contact_purposes vsccp on vsccp.sms_communication_id = sc.id
                                               left join crm.vw_sms_communication_activity vsca on vsca.sms_communication_id = sc.id
                                               join
                                           crm.sms_communication_customers scc
                                           on scc.sms_communication_id = sc.id
                                               and scc.customer_detail_id = :customerDetailId
                                               left join
                                           crm.sms_communication_customer_contacts sccc
                                           on
                                               sccc.sms_communication_customer_id = scc.id
                                               join customer.customer_communications cc
                                                    on scc.customer_communication_id = cc.id
                                               join
                                           nomenclature.communication_topics ct
                                           on sc.communication_topic_id = ct.id
                                               join
                                           customer.customer_details cd
                                           on scc.customer_detail_id = cd.id
                                               join
                                           customer.customers c
                                           on cd.customer_id = c.id
                                               left join
                                           nomenclature.legal_forms lf
                                           on cd.legal_form_id = lf.id
                                               join
                                           customer.account_managers am
                                           on sc.system_user_id = am.user_name
                                               left join
                                           customer.account_managers am2
                                           on sc.sender_employee_id = am2.id
                                               left join
                                           crm.sms_communication_contact_purposes sccp
                                           on sccp.sms_communication_id = sc.id
                                               and sccp.status = 'ACTIVE'
                                               left join
                                           nomenclature.contact_purposes cp
                                           on sccp.contact_purpose_id = cp.id
                                               left join
                                           crm.sms_communication_activity sca
                                           on sca.sms_communication_id= sc.id
                                               and sca.status = 'ACTIVE'
                                               left join
                                           activity.activity act
                                           on sca.activity_id = act.id
                                               left join
                                           crm.sms_communication_tasks sct
                                           on sct.sms_communication_id = sc.id
                                               and sct.status = 'ACTIVE'
                                       where sc.communication_channel = 'MASS_SMS' and
                                           ((:entityStatuses) is null or text(sc.status)  in :entityStatuses)
                                         and
                                           ((:creatorEmployeeId) is null or am.id  in :creatorEmployeeId)
                                         and
                                           ((:senderEmployeeId) is null or am2.id  in :senderEmployeeId)
                                         and
                                           (date(:createDateFrom) is null or date(sc.create_date) >= date(:createDateFrom))
                                         and
                                           (date(:createDateTo) is null or date(sc.create_date) <= date(:createDateTo))
                                         and
                                           ((:contactPurposeId) is null or cp.id in :contactPurposeId)
                                         and
                                           ((:communicationType) is null or text(sc.communication_type)  in :communicationType)
                                         and
                                           ((:activityId) is null or act.activity_id in :activityId)
                                         and
                                           ((:taskId) is null or sct.task_id in :taskId)
                                         and
                                           ((:communicationTopicId) is null or sc.communication_topic_id  in :communicationTopicId)
                                         and
                                           ((:communicationStatus) is null or text(sc.communication_status)  in :communicationStatus)
                                         and (:prompt is null or (:searchBy = 'ALL' and (
                                           text(scc.id) like :prompt
                                               or
                                           text(sc.id) like :prompt
                                               or
                                           lower(text(cc.contact_type_name)) like :prompt
                                               or
                                           lower(text(cd.name)) like :prompt
                                               or
                                           lower(text(c.identifier)) like :prompt
                                               or
                                           lower(text(c.customer_number)) like :prompt
                                               or
                                           lower(text(sccc.phone_number)) like :prompt
                                           )
                                           )
                                           or (
                                                  (:searchBy = 'COMMUNICATION_ID' and text(scc.id) like :prompt)
                                                      or
                                                  (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and text(sc.id) like :prompt)
                                                      or
                                                  (:searchBy = 'COMMUNICATION_DATA' and lower(text(cc.contact_type_name)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_NAME' and lower(text(cd.name)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                                                      or
                                                  (:searchBy = 'PHONE_NUMBER' and lower(text(sccc.phone_number)) like :prompt)
                                                  ))
                                       union all
                                       select distinct
                                           scc.id,
                                           case when sc.communication_channel =  'MASS_SMS' then sc.id else null end,
                                           case when lower(text(:activityDirection)) = 'desc'
                                                    then
                                                    vsca.activityDesc
                                                else
                                                    vsca.activityAsc
                                               end as activity,
                                           am.display_name as creatorEmployee,
                                           am2.display_name as senderEmployee,
                                           case when lower(text(:contactPurposeDirection)) = 'desc'
                                                    then
                                                    vsccp.contactPurposeDesc
                                                else
                                                    vsccp.contactPurposeAsc
                                               end as contactPurpose,
                                           text(sc.communication_type) as communicationType,
                                           cc.contact_type_name as communicationData,
                                           sc.sent_date as sentReceiveDate,
                                           sc.create_date as createDate,
                                           ct.name as communicationTopic,
                                           text(coalesce(sc.communication_status,scc.sms_comm_status)) as communicationStatus,
                                           text(sc.status) as status,
                                           'SMS' as communicationChannel,
                                           'SMS',
                                           null
                                       from
                                           crm.sms_communications sc
                                               left join crm.vw_sms_communication_contact_purposes vsccp on vsccp.sms_communication_id = sc.id
                                               left join crm.vw_sms_communication_activity vsca on vsca.sms_communication_id = sc.id
                                               join
                                           crm.sms_communication_customers scc
                                           on scc.sms_communication_id = sc.id
                                               and scc.customer_detail_id = :customerDetailId
                                               left join
                                           crm.sms_communication_customer_contacts sccc
                                           on
                                               sccc.sms_communication_customer_id = scc.id
                                               join customer.customer_communications cc
                                                    on scc.customer_communication_id = cc.id
                                               join
                                           nomenclature.communication_topics ct
                                           on sc.communication_topic_id = ct.id
                                               join
                                           customer.customer_details cd
                                           on scc.customer_detail_id = cd.id
                                               join
                                           customer.customers c
                                           on cd.customer_id = c.id
                                               left join
                                           nomenclature.legal_forms lf
                                           on cd.legal_form_id = lf.id
                                               join
                                           customer.account_managers am
                                           on sc.system_user_id = am.user_name
                                               left join
                                           customer.account_managers am2
                                           on sc.sender_employee_id = am2.id
                                               left join
                                           crm.sms_communication_contact_purposes sccp
                                           on sccp.sms_communication_id = sc.id
                                               and sccp.status = 'ACTIVE'
                                               left join
                                           nomenclature.contact_purposes cp
                                           on sccp.contact_purpose_id = cp.id
                                               left join
                                           crm.sms_communication_activity sca
                                           on sca.sms_communication_id= sc.id
                                               and sca.status = 'ACTIVE'
                                               left join
                                           activity.activity act
                                           on sca.activity_id = act.id
                                               left join
                                           crm.sms_communication_tasks sct
                                           on sct.sms_communication_id = sc.id
                                               and sct.status = 'ACTIVE'
                                       where
                                           ((:entityStatuses) is null or text(sc.status)  in :entityStatuses)
                                         and
                                           ((:creatorEmployeeId) is null or am.id  in :creatorEmployeeId)
                                         and
                                           ((:senderEmployeeId) is null or am2.id  in :senderEmployeeId)
                                         and
                                           (date(:createDateFrom) is null or date(sc.create_date) >= date(:createDateFrom))
                                         and
                                           (date(:createDateTo) is null or date(sc.create_date) <= date(:createDateTo))
                                         and
                                           ((:contactPurposeId) is null or cp.id in :contactPurposeId)
                                         and
                                           ((:communicationType) is null or text(sc.communication_type)  in :communicationType)
                                         and
                                           ((:activityId) is null or act.activity_id in :activityId)
                                         and
                                           ((:taskId) is null or sct.task_id in :taskId)
                                         and
                                           ((:communicationTopicId) is null or sc.communication_topic_id  in :communicationTopicId)
                                         and
                                           ((:communicationStatus) is null or text(coalesce(sc.communication_status,scc.sms_comm_status))  in :communicationStatus)
                                         and (:prompt is null or (:searchBy = 'ALL' and (
                                           text(scc.id) like :prompt
                                               or
                                           text(sc.id) like :prompt
                                               or
                                           lower(text(cc.contact_type_name)) like :prompt
                                               or
                                           lower(text(cd.name)) like :prompt
                                               or
                                           lower(text(c.identifier)) like :prompt
                                               or
                                           lower(text(c.customer_number)) like :prompt
                                               or
                                           lower(text(sccc.phone_number)) like :prompt
                                           )
                                           )
                                           or (
                                                  (:searchBy = 'COMMUNICATION_ID' and text(scc.id) = :prompt)
                                                      or
                                                  (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and sc.communication_channel = 'MASS_SMS' and text(scc.sms_communication_id) = :prompt)
                                                      or
                                                  (:searchBy = 'COMMUNICATION_DATA' and lower(text(cc.contact_type_name)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_NAME' and lower(text(cd.name)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                                      or
                                                  (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                                                      or
                                                  (:searchBy = 'PHONE_NUMBER' and lower(text(sccc.phone_number)) like :prompt)
                                                  ))
                                   ) as sms) as rel
                       where
                           ((:kindOfCommunication) is null or text(rel.communicationChannel)  in (:kindOfCommunication))
                         and
                           ((:relationType) is null or text(rel.relationType)  in (:relationType))
            
            """)
    Page<CustomerRelatedRelationshipMiddleResponse> getCustomerRelatedCommunication(
            @Param("customerDetailId") Long customerDetailId,
            @Param("entityStatuses") List<String> entityStatuses,
            @Param("creatorEmployeeId") List<Long> creatorEmployeeId,
            @Param("senderEmployeeId") List<Long> senderEmployeeId,
            @Param("createDateFrom") LocalDateTime createDateFrom,
            @Param("createDateTo") LocalDateTime createDateTo,
            @Param("contactPurposeId") List<Long> contactPurposeId,
            @Param("communicationType") List<String> communicationType,
            @Param("activityId") List<Long> activityId,
            @Param("taskId") List<Long> taskId,
            @Param("communicationTopicId") List<Long> communicationTopicId,
            @Param("communicationStatus") List<String> communicationStatus,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("activityDirection") String activityDirection,
            @Param("contactPurposeDirection") String contactPurposeDirection,
            @Param("kindOfCommunication") List<String> kindOfCommunication,
            @Param("relationType") List<String> relationType,
            Pageable pageable

    );

    @Query("""
                select new bg.energo.phoenix.model.documentModels.mlo.Manager(t.name,m.name,m.surname,m.jobPosition)
                from CustomerDetails cd
                left join Manager m on m.customerDetailId=cd.id
                join Title t on m.title=t
                where cd.id=:customerDetailId
            """)
    List<Manager> getManagersByCustomer(Long customerDetailId);

    @Query(nativeQuery = true,
            value = """
                        WITH LatestCustomerDetails AS (SELECT c.id AS customer_id, MAX(cd.id) AS max_customer_detail_id
                                                       FROM customer.customers c
                                                                JOIN customer.customer_details cd ON c.id = cd.customer_id
                                                       WHERE c.id = :customerId
                                                       GROUP BY c.id),
                             LatestCustomerCommunications AS (SELECT cc.customer_detail_id, MAX(cc.id) AS max_customer_communication_id
                                                              FROM customer.customer_communications cc
                                                                       JOIN LatestCustomerDetails lcd
                                                                            ON lcd.max_customer_detail_id = cc.customer_detail_id
                                                                       join customer.customer_comm_contact_purposes p
                                                                            on cc.id = p.customer_communication_id
                                                              WHERE cc.status = 'ACTIVE'
                                                                and p.contact_purpose_id = :purposeId
                                                              GROUP BY cc.customer_detail_id)
                        SELECT distinct cc.id as id, STRING_AGG(DISTINCT ccc.contact_value, ';') AS contactValues
                        FROM customer.customer_comm_contact_purposes cccp
                                 JOIN customer.customers c ON c.id = :customerId
                                 JOIN customer.customer_details cd ON c.id = cd.customer_id
                                 JOIN customer.customer_communications cc ON cc.id = cccp.customer_communication_id
                                 JOIN LatestCustomerDetails lcd ON lcd.customer_id = c.id AND lcd.max_customer_detail_id = cd.id
                                 JOIN LatestCustomerCommunications lcc
                                      ON lcc.customer_detail_id = cd.id AND lcc.max_customer_communication_id = cc.id
                                 JOIN customer.customer_communication_contacts ccc ON ccc.customer_communication_id = cc.id
                        WHERE cc.customer_detail_id = cd.id
                          AND cccp.status = 'ACTIVE'
                          AND ccc.status = 'ACTIVE'
                          AND text(ccc.contact_type) = :contactType
                          AND ccc.contact_value IS NOT NULL
                        group by cc.id
                    """
    )
    CommunicationDataMiddleResponse getCustomerCommunicationDataListForDocument(
            @Param("customerId") Long customerId,
            @Param("purposeId") Long purposeId,
            @Param("contactType") String contactType
    );

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                c.customerNumber,
                text(c.customerNumber)
            )
            from Customer c
            where text(c.customerNumber) like :prompt
            """)
    List<ShortResponse> getCustomerNumbers(
            @Param("prompt")
            String prompt,
            PageRequest pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse(
                c.customerNumber,
                text(c.customerNumber)
            )
            from Customer c
            where c.customerNumber in :ids
            """)
    List<ConditionParameterResponse> findByIdIn(List<Long> ids);
}
