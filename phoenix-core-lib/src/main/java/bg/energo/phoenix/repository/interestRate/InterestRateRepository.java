package bg.energo.phoenix.repository.interestRate;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateCharging;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateType;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateListResponse;
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

@Repository
public interface InterestRateRepository extends JpaRepository<InterestRate, Long> {
    Optional<InterestRate> findByIsDefaultAndStatus(Boolean isDefault, InterestRateStatus status);

    Optional<InterestRate> findByIdAndStatusIn(Long id, List<InterestRateStatus> statuses);

    boolean existsByIdAndStatusIn(Long id, List<InterestRateStatus> statuses);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.InterestRate.InterestRateListResponse(
                   ir.id,
                   ir.name,
                   ir.charging,
                   ir.type,
                   ir.grouping,
                   ir.isDefault,
                   ir.createDate,
                   ir.status)
            from
             InterestRate ir
            where
             ir.status in :status
              and
             (coalesce(:interestcharging,'0') = '0' or  ir.charging  in :interestcharging)
              and
              ir.grouping  in :groupingoftheinterest
              and
             (coalesce(:interestratetype,'0') = '0' or  ir.type  in :interestratetype)
              and
             (:columnvalue is null or (:columnname =  'ALL' and (
                                                                 lower(ir.name) like :columnvalue
                                                                 or
                                                                 text(ir.id)  like :columnvalue
                                                                )
                                                              )
                                                          or (
                                                                (:columnname = 'NAME' and Lower(ir.name)  like :columnvalue)
                                                                 or
                                                                (:columnname = 'ID' and text(ir.id)  like :columnvalue)
                                                             )
                                                             )
            """)
    Page<InterestRateListResponse> list(@Param("columnvalue") String prompt,
                                        @Param("columnname") String searchField,
                                        @Param("status") List<InterestRateStatus> status,
                                        @Param("interestcharging") List<InterestRateCharging> charging,
                                        @Param("groupingoftheinterest") List<Boolean> grouping,
                                        @Param("interestratetype") List<InterestRateType> InterestRateType,
                                        Pageable pageable);


    @Query(
            value = """
                    select ir from InterestRate ir
                        where ir.status = 'ACTIVE'
                        and (:prompt is null or lower(ir.name) like :prompt)
                        order by ir.isDefault desc, ir.createDate
                    """
    )
    Page<InterestRate> findAvailableInterestRatesForContracts(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    Optional<InterestRate> findByNameAndStatusIn(String name, List<InterestRateStatus> status);

    @Query("""
            select count(pcd.id) > 0
            from InterestRate ir
            join ProductContractDetails pcd on pcd.applicableInterestRate = ir.id
            join ProductContract pc on pc.id = pcd.contractId
            where ir.id = :id
            and pc.status = 'ACTIVE'
            """)
    boolean hasConnectionWithProductContract(Long id);

    @Query("""
            select count(scd.id) > 0
            from InterestRate ir
            join ServiceContractDetails scd on scd.applicableInterestRate = ir.id
            join ServiceContracts sc on sc.id = scd.contractId
            where ir.id = :id
            and sc.status = 'ACTIVE'
            """)
    boolean hasConnectionWithServiceContract(Long id);

    @Query("""
            select count(so.id) > 0
            from InterestRate ir
            join ServiceOrder so on so.applicableInterestRateId = ir.id
            where ir.id = :id
            and so.status = 'ACTIVE'
            """)
    boolean hasConnectionWithServiceOrder(Long id);

    @Query("""
            select count(go.id) > 0
            from InterestRate ir
            join GoodsOrder go on go.applicableInterestRateId = ir.id
            where ir.id = :id
            and go.status = 'ACTIVE'
            """)
    boolean hasConnectionWithGoodsOrder(Long id);

    @Query("""
           select new bg.energo.phoenix.model.CacheObject(ir.id,ir.name)
           from InterestRate ir
           where ir.name=:name
           and ir.status=:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findCacheObjectByName(String name, InterestRateStatus status);

    Optional<InterestRate> findFirstByNameAndStatus(String name,InterestRateStatus status);

    Optional<InterestRate> findFirstByNameAndIdNotInAndStatus(String name,List<Long> id,InterestRateStatus status);
}
