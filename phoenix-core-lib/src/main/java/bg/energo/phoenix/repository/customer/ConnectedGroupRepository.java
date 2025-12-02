package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.customer.ConnectedGroup;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.customer.ConnectedGroupFilterResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConnectedGroupRepository extends JpaRepository<ConnectedGroup, Long>, JpaSpecificationExecutor<ConnectedGroup> {

    @Query("""
                    select g from ConnectedGroup g
                    where g.id = :id
                    and g.status in :statuses
            """)
    Optional<ConnectedGroup> findByIdAndStatus(@Param("id") Long id, @Param("statuses") List<Status> statuses);

    @Query(
            value = """
                    select count(cg.id) > 0 from ConnectedGroup cg
                        where cg.name = :name
                        and cg.status in :statuses
                        and (:id is null or cg.id <> :id)
                    """
    )
    boolean existsByNameAndStatusInAndIdNot(String name, List<Status> statuses, Long id);

    @Query("""
            select g from ConnectedGroup g
            where lower(g.name) like :prompt
            and g.status in :statuses
                """)
    Page<ConnectedGroup> filterByNameAndStatus(
            @Param("prompt") String prompt,
            @Param("statuses") List<Status> statuses,
            Pageable pageable);

    @Query("""
            select g from ConnectedGroup g
            join CustomerConnectedGroup ccg on ccg.connectedGroupId = g.id
            where ccg.customerId = :customerId
            and ccg.status in :statuses
            order by g.createDate
            """)
    List<ConnectedGroup> findByCustomerIdAndStatus(
            @Param("customerId") Long customerId,
            @Param("statuses") List<Status> connectionStatus
    );


    @Query(
            nativeQuery = true,
            value = """
                    select
                        g.id as id,
                        g.name as name,
                        vwg.display_name as managers,
                        g.create_date as created,
                        ct.name as type,
                        g.status as status,
                        (select count(1) from customer.customer_connected_groups con where con.connected_group_id = g.id and con.status = 'ACTIVE') as count
                    FROM customer.connected_groups g
                             left join nomenclature.connection_types_gcc ct on g.connection_types_gcc_id = ct.id
                             left join customer.vw_customer_group_account_managers vwg on g.id =vwg.connected_group_id
                             left JOIN customer.customer_connected_groups ccg ON g.id = ccg.connected_group_id and ccg.status <> 'DELETED'
                             left JOIN customer.customers c ON ccg.customer_id = c.id
                             left JOIN customer.customer_details cd ON c.id = cd.customer_id
                             left JOIN customer.customer_account_managers cam ON cd.id = cam.customer_detail_id
                             left JOIN customer.account_managers am ON cam.account_manager_id = am.id
                    where (:searchField is null or :searchValue is null
                        or (
                                   ('ALL' = :searchField AND
                                    (
                                                lower(g.name) like lower(concat('%', :searchValue, '%'))
                                            or lower(cd.name) like lower(concat('%', :searchValue, '%'))
                                            or lower(cd.middle_name) like lower(concat('%', :searchValue, '%'))
                                            or lower(cd.last_name) like lower(concat('%', :searchValue, '%'))
                                            or lower(am.display_name) like lower(concat('%', :searchValue, '%'))
                                            or text(c.identifier) = :searchValue
                                            or text(c.customer_number) = :searchValue
                                        )
                                       )
                                   or ('GROUP_NAME' = :searchField AND (lower(g.name) like lower(concat('%', :searchValue, '%'))))
                                   or ('CUSTOMER_NAME' = :searchField AND (lower(cd.name) like lower(concat('%', :searchValue, '%'))))
                                   or ('LEGAL_NAME' = :searchField AND (lower(cd.name) like lower(concat('%', :searchValue, '%'))) and c.customer_type='LEGAL_ENTITY')
                                   or ('CUSTOMER_MNAME' = :searchField AND (lower(cd.middle_name) like lower(concat('%', :searchValue, '%'))))
                                   or ('CUSTOMER_LNAME' = :searchField AND (lower(cd.last_name) like lower(concat('%', :searchValue, '%'))))
                                   or ('MANAGER_NAME' = :searchField AND (lower(am.display_name) like lower(concat('%', :searchValue, '%'))))
                                   or ('CUSTOMER_IDENTIFIER' = :searchField AND text(c.identifier) = :searchValue)
                                   or ('CUSTOMER_NUMBER' = :searchField AND text(c.customer_number) = :searchValue)
                               )
                        )
                      and ((:connectionId) is null or (g.connection_types_gcc_id in (:connectionId)))
                      and (
                            (:customerCountTo is null or (select count(ccg.id)
                                                          from customer.customer_connected_groups ccg
                                                          where ccg.connected_group_id = g.id
                                                            and ccg.status = 'ACTIVE') <= :customerCountTo)
                            and (:customerCountFrom is null or( select count(ccg.id)
                                                                from customer.customer_connected_groups ccg
                                                                where ccg.connected_group_id = g.id
                                                                  and ccg.status = 'ACTIVE') >= :customerCountFrom)
                        )
                      and text(g.status) in (:statuses)
                    group by g.id, ct.name, vwg.display_name
                    """,
            countQuery = """
            select count(1)
            FROM customer.connected_groups g
                     left join nomenclature.connection_types_gcc ct on g.connection_types_gcc_id = ct.id
                     left join customer.vw_customer_group_account_managers vwg on g.id = vwg.connected_group_id
                     left JOIN customer.customer_connected_groups ccg ON g.id = ccg.connected_group_id and ccg.status <> 'DELETED'
                     left JOIN customer.customers c ON ccg.customer_id = c.id
                     left JOIN customer.customer_details cd ON c.id = cd.customer_id
                     left JOIN customer.customer_account_managers cam ON cd.id = cam.customer_detail_id
                     left JOIN customer.account_managers am ON cam.account_manager_id = am.id
            where (
                    :searchField is null or :searchValue is null or (
                        ('ALL' = :searchField AND
                         (lower(g.name) like lower(concat('%', :searchValue, '%')) or
                          lower(cd.name) like lower(concat('%', :searchValue, '%')) or
                          lower(cd.middle_name) like lower(concat('%', :searchValue, '%')) or
                          lower(cd.last_name) like lower(concat('%', :searchValue, '%')) or
                          lower(am.display_name) like lower(concat('%', :searchValue, '%'))
                           or text(c.identifier) = :searchValue
                            or text(c.customer_number) = :searchValue
                             )) or
                        ('GROUP_NAME' = :searchField AND (lower(g.name) like lower(concat('%', :searchValue, '%')))) or
                        ('CUSTOMER_NAME' = :searchField AND (lower(cd.name) like lower(concat('%', :searchValue, '%')))) or
                        ('LEGAL_NAME' = :searchField AND (lower(cd.name) like lower(concat('%', :searchValue, '%'))) and
                         c.customer_type = 'LEGAL_ENTITY') or
                        ('CUSTOMER_MNAME' = :searchField AND (lower(cd.middle_name) like lower(concat('%', :searchValue, '%')))) or
                        ('CUSTOMER_LNAME' = :searchField AND (lower(cd.last_name) like lower(concat('%', :searchValue, '%')))) or
                        ('MANAGER_NAME' = :searchField AND (lower(am.display_name) like lower(concat('%', :searchValue, '%'))))
                         or ('CUSTOMER_IDENTIFIER' = :searchField AND text(c.identifier) = :searchValue)
                                   or ('CUSTOMER_NUMBER' = :searchField AND text(c.customer_number) = :searchValue)
                    )
                )
              and (
                    (:connectionId) is null
                    or (g.connection_types_gcc_id in (:connectionId))
                )
              and (
                    (:customerCountTo is null or (select count(ccg.id)
                                                  from customer.customer_connected_groups ccg
                                                  where ccg.connected_group_id = g.id
                                                    and ccg.status = 'ACTIVE') <= :customerCountTo)
                    and (
                            :customerCountFrom is null or (select count(ccg.id)
                                                           from customer.customer_connected_groups ccg
                                                           where ccg.connected_group_id = g.id
                                                             and ccg.status = 'ACTIVE') >= :customerCountFrom))
              and text(g.status) in (:statuses)
            
            group by g.id, ct.name, vwg.display_name
            """
    )
    Page<ConnectedGroupFilterResponse> filter(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("connectionId") List<Long> connectionId,
            @Param("customerCountFrom") Long customerCountFrom,
            @Param("customerCountTo") Long customerCountTo,
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );

}
