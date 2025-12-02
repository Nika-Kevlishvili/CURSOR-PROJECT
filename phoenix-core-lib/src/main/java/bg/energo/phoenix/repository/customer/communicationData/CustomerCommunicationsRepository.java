package bg.energo.phoenix.repository.customer.communicationData;

import bg.energo.phoenix.model.documentModels.latePaymentFine.CommunicationDataMiddleResponse;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceCommunicationResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.communicationData.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CustomerCommunicationsRepository extends JpaRepository<CustomerCommunications, Long> {

    @Query(
            """
                        select cc from CustomerCommunications as cc
                            where cc.id = :id
                            and cc.status in :statuses
                    """
    )
    Optional<CustomerCommunications> findByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<Status> statuses
    );

    Optional<CustomerCommunications> findByIdAndCustomerDetailsIdAndStatus(Long id, Long customerDetailsId, Status status);

    @Query(
            """
                        select new bg.energo.phoenix.model.response.customer.communicationData.LocalAddressInfo(
                            c.id,
                            c.name,
                            r.id,
                            r.name,
                            m.id,
                            m.name,
                            pp.id,
                            pp.name,
                            zc.id,
                            zc.name,
                            d.id,
                            d.name,
                            ra.id,
                            ra.name,
                            cc.residentialAreaType,
                            s.id,
                            s.name,
                            cc.streetType
                        )
                        from CustomerCommunications as cc
                        left join PopulatedPlace as pp on pp.id = cc.populatedPlaceId
                        left join Municipality as m on m.id = pp.municipality.id
                        left join Region as r on r.id = m.region.id
                        left join Country as c on c.id = cc.countryId
                        left join ZipCode as zc on zc.id = cc.zipCodeId
                        left join District as d on d.id = cc.districtId
                        left join ResidentialArea as ra on ra.id = cc.residentialAreaId
                        left join Street as s on s.id = cc.streetId
                            where cc.id = :id
                    """
    )
    LocalAddressInfo getLocalAddressInfo(
            @Param("id") Long id
    );

    @Query(
            """
                        select new bg.energo.phoenix.model.response.customer.communicationData.ForeignAddressInfo(
                            c.id,
                            c.name,
                            cc.regionForeign,
                            cc.municipalityForeign,
                            cc.populatedPlaceForeign,
                            cc.zipCodeForeign,
                            cc.districtForeign,
                            cc.residentialAreaTypeForeign,
                            cc.residentialAreaForeign,
                            cc.streetTypeForeign,
                            cc.streetForeign
                        )
                        from CustomerCommunications as cc
                        left join Country as c on c.id = cc.countryId
                            where cc.id = :id
                    """
    )
    ForeignAddressInfo getForeignAddressInfo(
            @Param("id") Long id
    );

    @Query(
            """
                        select cc from CustomerCommunications as cc
                            where cc.customerDetailsId = :customerDetailId
                            and cc.status in :statuses
                            order by cc.createDate
                    """
    )
    List<CustomerCommunications> findByCustomerDetailIdAndStatuses(
            @Param("customerDetailId") Long customerDetailId,
            @Param("statuses") List<Status> statuses
    );

    @Query(nativeQuery = true,
            value = """
                    (select cc.*
                    from customer.customer_communications as cc
                             join customer.customer_comm_contact_purposes as cccp on cccp.customer_communication_id = cc.id
                    where cc.customer_detail_id = :customerDetailId
                      and cc.status = 'ACTIVE'
                      and cccp.contact_purpose_id = :billingId
                      and cccp.status='ACTIVE'
                     order by create_date desc
                     limit 1)
                    union
                    (select cc.*
                    from customer.customer_communications as cc
                             join customer.customer_comm_contact_purposes as cccp on cccp.customer_communication_id = cc.id
                    where cc.customer_detail_id = :customerDetailId
                      and cc.status = 'ACTIVE'
                      and cccp.contact_purpose_id = :contactId
                      and cccp.status='ACTIVE'
                    order by create_date desc
                    limit 1)
                                  
                                 
                                 """
    )
    List<CustomerCommunications> findCommunicationWithBillingAndContract(
            @Param("customerDetailId") Long customerDetailId,
            Long billingId,
            Long contactId
    );


    @Query(
            """
                        select cc from CustomerCommunications as cc
                            where cc.customerDetailsId = :customerDetailId
                            and cc.status in :statuses
                    """
    )
    List<CustomerCommunications> getCustomerCommunicationIdsByCustomerDetailId(
            @Param("customerDetailId") Long id,
            @Param("statuses") List<Status> statuses
    );

    boolean existsByIdAndStatusIn(Long id, List<Status> statuses);

    @Query("""
                                select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                                cc.id,
                                cc.contactTypeName,
                                cc.createDate
                            )
                            from CustomerCommContactPurposes cccp
                            join ContactPurpose cp on cccp.contactPurposeId = cp.id
                            join CustomerCommunications cc on cc.id=cccp.customerCommunicationsId
                            where cc.id = :id
                            and cc.customerDetailsId = :customerDetailsId
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
            """)
    Optional<CustomerCommunicationDataResponse> findByIdAndCustomerDetailsIdAndPurpose(@Param("id") Long customerCommunicationId,
                                                                                       @Param("customerDetailsId") Long customerDetailsId,
                                                                                       @Param("purposeId") Long billingCommunicationId);

    @Query("""
                select new bg.energo.phoenix.model.response.billing.invoice.InvoiceCommunicationResponse(
                    cc.id,
                    concat(cc.contactTypeName, ' (', cp.name, ')')
                )
                from CustomerCommunications cc
                    join CustomerCommContactPurposes cccp on
                        (cccp.customerCommunicationsId = cc.id and cccp.status = 'ACTIVE')
                    join ContactPurpose cp on (cccp.contactPurposeId = cp.id)
                where cc.id = :id
                and cc.status = 'ACTIVE'
                and cp.id = :purposeId
            """
    )
    Optional<InvoiceCommunicationResponse> findByIdForInvoice(@Param("id") Long id,
                                                              @Param("purposeId") Long purposeId);

    @Query("""
                    select new bg.energo.phoenix.model.response.customer.communicationData.CustomerCommunicationMobileDataResponse(cc.id,cc.contactTypeName,cc.createDate,ccc.contactValue) from CustomerCommunications cc
                    join CustomerCommunicationContacts ccc on ccc.customerCommunicationsId = cc.id
                    and ccc.status='ACTIVE'
                    and ccc.contactType='MOBILE_NUMBER'
                    and ccc.sendSms=true
                    and cc.status='ACTIVE'
                    and cc.customerDetailsId=:customerDetailId
                    order by cc.createDate desc
            """
    )
    List<CustomerCommunicationMobileDataResponse> findMobileContactByCommunicationId(@Param("customerDetailId") Long customerDetailId);

    @Query("""
                select new bg.energo.phoenix.model.response.customer.communicationData.CustomerCommunicationEmailDataResponse(
                                cc.id,
                                cc.contactTypeName,
                                cc.createDate,
                                ccc.contactValue
                                )
            from CustomerCommunications cc
            join CustomerCommunicationContacts ccc on ccc.customerCommunicationsId = cc.id
            where ccc.status = 'ACTIVE'
              and ccc.contactType = 'EMAIL'
              and cc.status = 'ACTIVE'
              and cc.customerDetailsId = :customerDetailId
              and cc.id in (
                  select cc_inner.id
                  from CustomerCommunications cc_inner
                  where cc_inner.customerDetailsId = :customerDetailId
                  and cc_inner.status = 'ACTIVE'
                  group by cc_inner.id, cc_inner.contactTypeName, cc_inner.createDate
                  order by cc_inner.createDate desc
                  limit 3
              )
            order by cc.createDate desc
                """
    )
    List<CustomerCommunicationEmailDataResponse> findEmailContactByCommunicationId(@Param("customerDetailId") Long customerDetailId);

    @Query("""
                    select cc,ccc,cccp.contactPurposeId from CustomerCommunications cc
                    join CustomerCommunicationContacts ccc on ccc.customerCommunicationsId=cc.id
                    join CustomerCommContactPurposes cccp on cccp.customerCommunicationsId=cc.id
                    and ccc.status='ACTIVE'
                    and ccc.contactType='MOBILE_NUMBER'
                    and ccc.sendSms=true
                    and cc.status='ACTIVE'
                    and cc.customerDetailsId=:customerDetailId
                    and cccp.contactPurposeId in :contactPurposeIds
                    and cccp.status='ACTIVE'
            """
    )
    List<Object[]> findCustomerCommunicationsByCustomerDetailIdAndPurpose(Long customerDetailId, Set<Long> contactPurposeIds);

    @Query("""
            select cc.id                   as communicationId,
                   ccc.id                  as contactId,
                   ccc.contactValue        as contactValue,
                   cccp.contactPurposeId   as contactPurposeId
            from CustomerCommunications cc
            join CustomerCommunicationContacts ccc on ccc.customerCommunicationsId = cc.id
            join CustomerCommContactPurposes cccp on cccp.customerCommunicationsId = cc.id
            and cc.customerDetailsId = :customerDetailId
            and cccp.contactPurposeId in(:contactPurposeIds)
            and ccc.contactType = 'EMAIL'
            and ccc.status = 'ACTIVE'
            and cc.status = 'ACTIVE'
            and cccp.status = 'ACTIVE'
            """
    )
    List<CustomerEmailCommDataMiddleResponse> findCustomerCommunicationsAndContactsByCustomerDetailIdAndPurpose(Long customerDetailId, Set<Long> contactPurposeIds);

    @Query("""
                select cc
                from CustomerLiability cl
                join Invoice inv on cl.invoiceId=inv.id
                join CustomerCommunications cc on
                COALESCE(inv.contractCommunicationId, inv.customerCommunicationId) = cc.id
                where cl.id=:liabilityId
            """)
    Optional<CustomerCommunications> findCustomerCommunicationByLiabilityId(@Param("liabilityId") Long liabilityId);

    @Query("""
                select ccc.contactValue
                from CustomerCommunications cc
                join CustomerCommunicationContacts ccc on cc.id = ccc.customerCommunicationsId
                where cc.id = :customerCommunicationId
                and ccc.contactType = "EMAIL"
            """)
    List<String> findEmailsByCustomerCommunication(@Param("customerCommunicationId") Long customerCommunicationId);

    @Query("""
                select cc
                from Invoice inv
                join CustomerCommunications cc on
                COALESCE(inv.contractCommunicationId, inv.customerCommunicationId) = cc.id
                where inv.id=:invoiceId
            """)
    Optional<CustomerCommunications> findCustomerCommunicationByInvoice(@Param("invoiceId") Long invoiceId);

    @Query(
            nativeQuery = true,
            value = """
                        select distinct cc.id as id, STRING_AGG(DISTINCT ccc.contact_value, ';') AS contactValues
                        from customer.customer_communications cc
                                 join customer.customer_communication_contacts ccc on cc.id = ccc.customer_communication_id
                        where cc.id = :customerCommunicationId
                          and ccc.contact_type = 'EMAIL'
                        group by cc.id
                    """)
    CommunicationDataMiddleResponse findCustomerCommunicationAndEmail(Long customerCommunicationId);

}
