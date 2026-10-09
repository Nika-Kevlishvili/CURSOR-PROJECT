package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.manager.CustomerManagerImport;
import bg.energo.migration.customer.models.CustomerManagerImportMiddleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerManagerImportRepository extends JpaRepository<CustomerManagerImport, Long> {

    @Query(nativeQuery = true, value = """
                    select distinct manager."Customer"                                                                as customerIdentifier,
                                        manager."Title-Manger1"                                                           as title1,
                                        manager."Name"                                                                    as name1,
                                        manager.middlename                                                                as middlename1,
                                        manager.surname                                                                   as surname1,
                                        manager."Position-Manger1"                                                        as position1,
                                        manager."Representation-Manager1"                                                 as representation1,
                                        manager."Personalnumber-Manager1"                                                 as personalNumber1,
                                        manager."Title-Manger2"                                                           as title2,
                                        split_part(manager."Name-Manger2", ' ', 1)                                        as name2,
                                        case
                                            when array_length(regexp_split_to_array("Name-Manger2", '\\s+'), 1) = 3
                                                then split_part(manager."Name-Manger2", ' ', 2)
                                            else ''
                                            end                                                                           as middlename2,
                                        split_part(manager."Name-Manger2", ' ',
                                                   array_length(regexp_split_to_array(manager."Name-Manger2", '\\s+'), 1)) as surname2,
                                        manager."Position-Manger2"                                                        as position2,
                                        manager."Representation-Manager2"                                                 as representaton2,
                                        manager."Title-Manger3"                                                           as title3,
                                        split_part(manager."Name-Manger3", ' ', 1)                                        as name3,
                                        case
                                            when array_length(regexp_split_to_array("Name-Manger3", '\\s+'), 1) = 3
                                                then split_part(manager."Name-Manger3", ' ', 2)
                                            else ''
                                            end                                                                           as middlename3,
                                        split_part(manager."Name-Manger3", ' ',
                                                   array_length(regexp_split_to_array(manager."Name-Manger3", '\\s+'), 1)) as surname3,
                                        manager."Position-Manger3"                                                        as position3,
                                        manager."Representation-Manager3"                                                 as representation3
                        from customer_migration.managers_for_import_tab2 manager
                                 join customer_migration.customers cust on cust.customer_identifier = manager."Customer"
                        where cust.customer_id = :customerId
            """)
    List<CustomerManagerImportMiddleResponse> getCustomerManagerImportByCustomer(@Param("customerId") String customerId);

    @Query(nativeQuery = true, value = """
        select t.id
        from  nomenclature.titles t
        where lower(t.name) = lower(:titleName)
    """)
    Long getTitelIdByTitle(@Param("titleName")String title);

    @Query(nativeQuery = true, value = """
        select r.id
        from nomenclature.representation_methods r
        where lower(r.name) = lower(:representation)
    """)
    Long getRepresentationIdByRepresentationName(@Param("representation")String representationName);
}
