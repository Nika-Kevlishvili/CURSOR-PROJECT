package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.CustomerGeneralData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerGeneralDataRepository extends JpaRepository<CustomerGeneralData, Long> {
    Optional<CustomerGeneralData> findByCustomerId(String customerId);

//    @Query(nativeQuery = true, value ="""
//        select c.*
//           from customer_migration.customers c
//           where c.customer_identifier ='000085577'
//
//    """)
    List<CustomerGeneralData> findAllByIdFromPhoenixIsNull();
    Slice<CustomerGeneralData> findAllByIdFromPhoenixIsNull(Pageable pageable);
    Slice<CustomerGeneralData> findAllByIdFromPhoenixIsNullAndCountryIdIsNotNull(Pageable pageable);


    @Query(nativeQuery = true, value ="""
        select customer.*
           from customer_migration.customers customer
           where customer.id_from_phoenix is null and customer.customer_identifier not in (
                select distinct req.customer_identifier
                from customer_migration.customer_creation_reqs req
                    left join customer_migration.customer_creation_resps resp on resp.request_id = req.id
                                                                        )
    """)
    Slice<CustomerGeneralData> findAllByIdFromPhoenixIsNullAndRequestNotSend(Pageable pageable);
}
