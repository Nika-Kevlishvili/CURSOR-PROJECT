package bg.energo.phoenix.repository.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import bg.energo.phoenix.model.response.receivable.collectionChannel.CollectionChannelListingMiddleResponse;
import bg.energo.phoenix.service.notifications.interfaces.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionChannelRepository extends Notification, JpaRepository<CollectionChannel, Long> {

    Optional<CollectionChannel> findCollectionChannelByIdAndStatusIn(Long id, List<EntityStatus> entityStatus);

    boolean existsByName(String name);

    @Query(nativeQuery = true,
            value =
                    """
                            select
                             cc.id as id,
                             cc.name as name,
                             cp.name as collectionPartner,
                             cc.type as type,
                             c.name as currency,
                             cc.status as status
                             from receivable.collection_channels cc
                             join nomenclature.collection_partners cp on cc.collection_partner_id = cp.id
                             join nomenclature.currencies c on cc.currency_id = c.id
                              where ((:collectionPartnerIds) is null or cc.collection_partner_id  in :collectionPartnerIds)
                              and ((:type) is null or text(cc.type) in :type)
                              and ((:currencyIds) is null or cc.currency_id  in :currencyIds)
                              and ((:statuses) is null or text(cc.status)  in :statuses)
                              and ((:statusesFromRequest) is null or text(cc.status)  in :statusesFromRequest)
                              and (:prompt is null or (:searchBy = 'ALL' and (
                                                         lower(cc.name) like :prompt
                                                          or
                                                          text(cc.id) like :prompt
                                                                 )
                                                             )
                                                             or (
                                                                 (:searchBy = 'NAME' and lower(cc.name) like :prompt)
                                                                  or
                                                                 (:searchBy = 'ID' and text(cc.id) like :prompt)
                                                             )
                                                         )
                                                                 """,
            countQuery = """
                    select count(1)
                    from receivable.collection_channels cc
                             join nomenclature.collection_partners cp on cc.collection_partner_id = cp.id
                             join nomenclature.currencies c on cc.currency_id = c.id
                              where ((:collectionPartnerIds) is null or cc.collection_partner_id  in :collectionPartnerIds)
                              and ((:type) is null or text(cc.type) in :type)
                              and ((:currencyIds) is null or cc.currency_id  in :currencyIds)
                              and ((:statuses) is null or text(cc.status)  in :statuses)
                              and ((:statusesFromRequest) is null or text(cc.status)  in :statusesFromRequest)
                              and (:prompt is null or (:searchBy = 'ALL' and (
                                                         lower(cc.name) like :prompt
                                                          or
                                                          text(cc.id) like :prompt
                                                                 )
                                                             )
                                                             or (
                                                                 (:searchBy = 'NAME' and lower(cc.name) like :prompt)
                                                                  or
                                                                 (:searchBy = 'ID' and text(cc.id) like :prompt)
                                                             )
                                                         )
                                        """
    )
    Page<CollectionChannelListingMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("collectionPartnerIds") List<Long> collectionPartnerIds,
            @Param("type") List<String> type,
            @Param("currencyIds") List<Long> currencyIds,
            @Param("statuses") List<String> statuses,
            @Param("searchBy") String searchBy,
            @Param("statusesFromRequest") List<String> statusesFromRequest,
            Pageable pageable
    );

    @Query("""
                    select c from CollectionChannel c
                    where c.status in :entityStatuses
                    and c.id=:collectionChannelId
            """)
    Optional<CollectionChannel> findByIdAndStatuses(Long collectionChannelId, List<EntityStatus> entityStatuses);

    @Query(value = """
                select mofb.condition_text from receivable.liabilities_condition_replacemets mofb
                where mofb.is_key = true
            """, nativeQuery = true)
    List<String> getConditionKeys();

    @Query(value = """
            Select receivable.liability_condition_eval_not_zero(:condition) as id
            """, nativeQuery = true)
    List<Long> getLiabilitiesByCondition(@Param("condition") String condition);

    @Query(nativeQuery = true,
            value =
                    """
                            select
                             cc.id as id,
                             cc.name as name,
                             cp.name as collectionPartner,
                             cc.type as type,
                             c.name as currency,
                             cc.status as status
                             from receivable.collection_channels cc
                             join nomenclature.collection_partners cp on cc.collection_partner_id = cp.id
                             join nomenclature.currencies c on cc.currency_id = c.id
                              where ((:collectionPartnerIds) is null or cc.collection_partner_id  in :collectionPartnerIds)
                              and ((:type) is null or text(cc.type) in :type)
                              and ((:currencyIds) is null or cc.currency_id  in :currencyIds)
                              and  text(cc.status)  = 'ACTIVE'
                              and (:prompt is null or (:searchBy = 'ALL' and (
                                                         lower(cc.name) like :prompt
                                                          or
                                                          text(cc.id) like :prompt
                                                                 )
                                                             )
                                                             or (
                                                                 (:searchBy = 'NAME' and lower(cc.name) like :prompt)
                                                                  or
                                                                 (:searchBy = 'ID' and text(cc.id) like :prompt)
                                                             )
                                                         )
                                                                 """,
            countQuery = """
                    select count(1)
                    from receivable.collection_channels cc
                             join nomenclature.collection_partners cp on cc.collection_partner_id = cp.id
                             join nomenclature.currencies c on cc.currency_id = c.id
                              where ((:collectionPartnerIds) is null or cc.collection_partner_id  in :collectionPartnerIds)
                              and ((:type) is null or text(cc.type) in :type)
                              and ((:currencyIds) is null or cc.currency_id  in :currencyIds)
                              and  text(cc.status)  = 'ACTIVE'
                              and (:prompt is null or (:searchBy = 'ALL' and (
                                                         lower(cc.name) like :prompt
                                                          or
                                                          text(cc.id) like :prompt
                                                                 )
                                                             )
                                                             or (
                                                                 (:searchBy = 'NAME' and lower(cc.name) like :prompt)
                                                                  or
                                                                 (:searchBy = 'ID' and text(cc.id) like :prompt)
                                                             )
                                                         )
                                        """
    )
    Page<CollectionChannelListingMiddleResponse> filterForPaymentMass(
            @Param("prompt") String prompt,
            @Param("collectionPartnerIds") List<Long> collectionPartnerIds,
            @Param("type") List<String> type,
            @Param("currencyIds") List<Long> currencyIds,
            @Param("searchBy") String searchBy,
            Pageable pageable
    );

    @Query("""
                    select cc from CollectionChannel cc
                    where cc.type='OFFLINE'
                    and cc.status='ACTIVE'
                    and (
                        cc.waitingPeriodTime is null
                        or cc.waitingPeriodTime < :currentTime
                    )
            """)
    List<CollectionChannel> findOfflineCollectionChannelsWhichAreNotMarked(LocalDateTime currentTime);

    @Query("""
                    select cc from CollectionChannel cc
                    where cc.type='OFFLINE'
                    and cc.status='ACTIVE'
                    and cc.waitingPeriodTime is not null
                    and cc.waitingPeriodTime >= :now
            """)
    List<CollectionChannel> findMarkedOfflineCollectionChannels(LocalDateTime now);

    List<CollectionChannel> findByTypeAndStatus(CollectionChannelType type, EntityStatus status);

    @Override
    @Query("""
            select am.id from Payment p
            join PaymentPackage pp on p.paymentPackageId = pp.id
            join CollectionChannel cc on cc.id = pp.collectionChannelId
            join AccountManager am on am.id = cc.employeeId
            where p.id=:paymentId
            and (:notificationState is null or :notificationState is not null)
            union
            select amt.accountManagerId from Payment p
            join PaymentPackage pp on p.paymentPackageId = pp.id
            join CollectionChannel cc on cc.id = pp.collectionChannelId
            join PortalTag pt on pt.id = cc.tagId
            join AccountManagerTag amt on amt.portalTagId = pt.id
            where p.id = :paymentId
            and (:notificationState is null or :notificationState is not null)
            """)
    List<Long> getNotificationTargets(Long paymentId, String notificationState);

    @Query(value =
            """
                    select channel.id 
                    from receivable.collection_channels channel 
                    where channel.name like :name 
                    and channel.status = 'ACTIVE'
                    and channel.type = 'ONLINE'
                    """,
            nativeQuery = true
    )
    Optional<Long> findCollectionChannelId(String name);
    @Query(value =
            """
                    select c 
                    from CollectionChannel c 
                    where c.name like :name 
                    and c.status = 'ACTIVE'
                    and c.type = 'ONLINE'
            """
    )
    Optional<CollectionChannel> findCollectionChanel(String name);
}
