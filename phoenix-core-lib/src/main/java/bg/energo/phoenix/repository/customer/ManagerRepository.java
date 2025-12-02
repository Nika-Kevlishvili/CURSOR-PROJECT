package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.documentModels.contract.response.ManagersResponse;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.customer.manager.ManagerBasicInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

    @Query(
            "select new bg.energo.phoenix.model.response.customer.manager.ManagerBasicInfo(m) " +
            " from Manager as m" +
            " where m.customerDetailId = :customerDetailId " +
            " and m.status = :status order by m.name "
    )
    List<ManagerBasicInfo> getManagersByCustomerDetailId(
            @Param("customerDetailId") Long customerDetailId,
            @Param("status") Status status
    );

    @Query(
            """
                       select m 
                       from Manager m
                       where m.customerDetailId = :customerDetailId 
                         and m.status = :status
                    """
    )
    List<Manager> getManagersByCustomerDetailIdAndStatus(
            @Param("customerDetailId") Long customerDetailId,
            @Param("status") Status status
    );

    @Query(
            "select m.id from Manager as m " +
            "where m.customerDetailId = :customerDetailId " +
            "and m.status = :status"
    )
    List<Long> getManagerIdsByCustomerDetailId(
            @Param("customerDetailId") Long customerDetailId,
            @Param("status") Status status
    );

    @Query(
            """
                    select m from Manager as m
                        where m.id = :id
                        and m.status in :statuses
                    """
    )
    Optional<Manager> findManagerByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<Status> statuses
    );

    @Query(
            """
                    select m from Manager as m
                        where m.customerDetailId = :customerDetailsId
                        and m.status in :statuses
                        order by m.createDate
                    """
    )
    List<Manager> findManagersByCustomerDetailId(
            @Param("customerDetailsId") Long customerDetailsId,
            @Param("statuses") List<Status> statuses
    );

    Optional<Manager> findByIdAndStatus(Long id, Status status);

    boolean existsByIdAndCustomerDetailIdAndStatusIn(Long id, Long customerDetailId, List<Status> statuses);

    List<Manager> findByCustomerDetailIdAndStatusOrderByCreateDateAsc(Long id, Status status);

    @Query(nativeQuery = true, value = """
            select cm.id           as Id,
                   t.name          as Title,
                   cm.name         as Name,
                   cm.surname      as Surname,
                   cm.job_position as JobPosition
            from customer.customer_managers cm
                     join customer.customer_details cd
                          on cm.customer_detail_id = cd.id
                     join product_contract.contract_details pcd on pcd.customer_detail_id = cd.id
                     join product_contract.contracts pc on pcd.contract_id = pc.id
                     join nomenclature.titles t on cm.title_id = t.id
            where cm.status = 'ACTIVE'
              and pc.id = :id
              and pcd.version_id = :versionId
            """)
    List<ManagersResponse> fetchManagersForProductContractDocument(Long id, Integer versionId);
    @Query(nativeQuery = true, value = """
            select cm.id           as Id,
                   t.name          as Title,
                   cm.name         as Name,
                   cm.surname      as Surname,
                   cm.job_position as JobPosition
            from customer.customer_managers cm
                     join customer.customer_details cd
                          on cm.customer_detail_id = cd.id
                     join service_contract.contract_details scd on scd.customer_detail_id = cd.id
                     join service_contract.contracts sc on scd.contract_id = sc.id
                     join nomenclature.titles t on cm.title_id = t.id
            where cm.status = 'ACTIVE'
              and sc.id = :id
              and scd.version_id = :versionId
            """)
    List<ManagersResponse> fetchManagersForServiceContractDocument(Long id, Long versionId);


}
