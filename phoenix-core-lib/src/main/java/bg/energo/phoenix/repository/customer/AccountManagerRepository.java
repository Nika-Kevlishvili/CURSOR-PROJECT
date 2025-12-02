package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.customer.customerAccountManager.AccountManagerResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>Account Manager Repository</h2>
 * Repository for entity {@link AccountManager}
 */
@Repository
public interface AccountManagerRepository extends JpaRepository<AccountManager, Long> {
    @Query("""
                select am
                from AccountManager as am
                where am.id = :id
                and am.status in :statuses
            """)
    Optional<AccountManager> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<Status> statuses
    );

    @Query("""
                select am
                from AccountManager as am
                where am.status in :statuses
            """)
    List<AccountManager> findByStatus(
            @Param("statuses") List<Status> statuses
    );


    Optional<AccountManager> findByUserNameAndStatusIn(String username, List<Status> statuses);

    Optional<AccountManager> findByIdAndStatusIn(Long id, List<Status> statuses);

    boolean existsByIdAndStatusIn(Long id, List<Status> statuses);


    @Query(
            value = """
                select new bg.energo.phoenix.model.response.customer.customerAccountManager.AccountManagerResponse(
                    am.id,
                    concat(am.displayName, ' (', am.userName, ')'),
                    am.userName,
                    am.firstName,
                    am.lastName,
                    am.displayName,
                    am.email,
                    am.organizationalUnit,
                    am.businessUnit,
                    am.status,
                    am.createDate
                )
                from AccountManager am
                where am.status in(:statuses)
                and (:prompt is null or (
                    lower(am.userName) like :prompt
                    or lower(am.lastName) like :prompt
                    or lower(am.firstName) like :prompt
                    or lower(am.displayName) like :prompt
                ))
                order by am.displayName
            """
    )
    Page<AccountManagerResponse> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<Status> statuses,
            Pageable pageable
    );

    Optional<AccountManager> findByUserName(String username);

    @Query("""
        select new bg.energo.phoenix.model.CacheObject(a.id, a.userName)
        from AccountManager a
        where a.userName = :username
        and a.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getByUsernameAndStatus(
            @Param("username") String username,
            @Param("status") Status status
    );


    @Query(
            value = """
                    select am.id from AccountManager as am
                        where am.status in :statuses
                        and am.id in :ids
                    """
    )
    List<Long> findByStatusInAndIdIn(
            @Param("statuses") List<Status> statuses,
            @Param("ids") List<Long> ids
    );


    @Query("""
            select new bg.energo.phoenix.model.CacheObject(am.id,am.userName) 
            from AccountManager am 
            where am.userName in (:employeeIdentifier)
            and am.status = 'ACTIVE'
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<CacheObject> findCacheObjectByNameIn(List<String> employeeIdentifier);

    @Query("""
            select new bg.energo.phoenix.model.CacheObject(am.id,am.userName) 
            from AccountManager am 
            where am.userName = :employeeIdentifier
            and am.status = 'ACTIVE'
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findCacheObjectByName(String employeeIdentifier);


    @Query("""
                    select new bg.energo.phoenix.model.CacheObject(m.id,m.name)
                    from Manager m
                    where m.customerDetailId=:customerDetailId
                    and m.status='ACTIVE'
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<CacheObject> findManagersByCustomerDetailId(Long customerDetailId);


    @Query("""
            select count(am.id) > 0 from AccountManager as am
            join AccountManagerTag tag on tag.accountManagerId = am.id
            where tag.portalTagId = :portalTagId
            and am.id=:managerId
            """)
    boolean managerHasTag(Long portalTagId, Long managerId);
}
