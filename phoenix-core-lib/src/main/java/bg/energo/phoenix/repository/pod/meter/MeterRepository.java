package bg.energo.phoenix.repository.pod.meter;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.pod.meter.Meter;
import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import bg.energo.phoenix.model.response.pod.meter.MeterListResponse;
import bg.energo.phoenix.model.response.pod.meter.MeterResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeterRepository extends JpaRepository<Meter, Long> {

    Optional<Meter> findByIdAndStatusIn(Long id, List<MeterStatus> statuses);

    List<Meter> findByNumberAndStatus(String meterNumber,MeterStatus statuses);
    @Query(
            value = """
                    select m from Meter as m
                        where m.number = :number
                        and m.gridOperatorId = :gridOperatorId
                        and (:id is null or m.id <> :id)
                        and m.status in :statuses
                    """
    )
    List<Meter> findByNumberAndGridOperatorAndStatusIn(
            @Param("number") String number,
            @Param("gridOperatorId") Long gridOperatorId,
            @Param("id") Long id,
            @Param("statuses") List<MeterStatus> statuses
    );


    @Query(
            value = """
                    select m from Meter as m
                        where m.podId = :podId
                        and m.gridOperatorId = :gridOperatorId
                        and (:id is null or m.id <> :id)
                        and m.status in :statuses
                    """
    )
    List<Meter> findByPodAndGridOperatorAndStatusIn(
            @Param("podId") Long podId,
            @Param("gridOperatorId") Long gridOperatorId,
            @Param("id") Long id,
            @Param("statuses") List<MeterStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.pod.meter.MeterResponse(
                        m.id,
                        m.number,
                        m.status,
                        go.id,
                        go.name,
                        pod,
                        podd,
                        m.installmentDate,
                        m.removeDate
                    )
                    from Meter m
                    join GridOperator go on go.id = m.gridOperatorId
                    join PointOfDelivery pod on pod.id = m.podId
                    join PointOfDeliveryDetails podd on podd.id = pod.lastPodDetailId
                        where m.id = :meterId
                    """
    )
    MeterResponse viewById(@Param("meterId") Long meterId);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.pod.meter.MeterListResponse(
                        m.id,
                        m.number,
                        pod.identifier,
                        go.name,
                        m.installmentDate,
                        m.removeDate,
                        m.status,
                        m.createDate
                    )
                    from Meter m
                    join PointOfDelivery pod on pod.id = m.podId
                    join PointOfDeliveryDetails podd on podd.id = pod.lastPodDetailId
                    join GridOperator go on go.id = m.gridOperatorId
                        where m.status in :statuses
                        and (coalesce(:gridOperatorIds, '0') = '0' or m.gridOperatorId in :gridOperatorIds)
                        and (cast(:installmentFrom as date) is null or m.installmentDate >= :installmentFrom)
                        and (cast(:installmentTo as date) is null or m.installmentDate <= :installmentTo)
                        and (cast(:removeFrom as date) is null or m.removeDate >= :removeFrom)
                        and (cast(:removeTo as date) is null or m.removeDate <= :removeTo)
                        and (:searchBy is null or
                            (:searchBy = 'ALL' and (lower(pod.identifier) like :prompt or cast(m.id as string) like :prompt or lower(m.number) like :prompt))
                            or (:searchBy = 'NUMBER' and lower(m.number) like :prompt)
                            or (:searchBy = 'POD_IDENTIFIER' and lower(pod.identifier) like :prompt)
                            or (:searchBy = 'ID' and cast(m.id as string) like :prompt)
                        )
                    """
    )
    Page<MeterListResponse> list(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("gridOperatorIds") List<Long> gridOperatorIds,
            @Param("installmentFrom") LocalDate installmentFrom,
            @Param("installmentTo") LocalDate installmentTo,
            @Param("removeFrom") LocalDate removeFrom,
            @Param("removeTo") LocalDate removeTo,
            @Param("statuses") List<MeterStatus> meterStatuses,
            Pageable pageable
    );


    @Query("""
                select new bg.energo.phoenix.model.CacheObject(m.id, m.number )
                from Meter m
                where m.id = :id
                and m.status =:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByNameAndStatus(@Param("id") Long id, @Param("status") MeterStatus status);


    @Query("""
            SELECT m FROM Meter m
            WHERE m.number = :number
                AND :gridOperatorId = m.gridOperatorId
                AND :periodFrom BETWEEN m.installmentDate AND COALESCE(m.removeDate, :periodFrom)
                AND :periodTo BETWEEN m.installmentDate AND COALESCE(m.removeDate, :periodTo)
                AND m.status = :status
            """)
    Optional<Meter> findByNumberAndGridOperatorIdAndStatus(LocalDate periodFrom, LocalDate periodTo,String number, Long gridOperatorId, MeterStatus status);


    Optional<List<Meter>> findByPodIdAndGridOperatorIdAndStatus(Long id, Long gridOperatorId, MeterStatus status);


    @Query("""
            select m from Meter m where m.installmentDate <= :installmentDate and (m.removeDate >= :removeDate or m.removeDate is null) and m.podId = :id
            and m.gridOperatorId = :gridOperatorId and m.status = :status
            """)
    Optional<List<Meter>> findByInstallmentDateLessThanEqualAndRemoveDateGreaterThanEqualAndPodIdAndGridOperatorIdAndStatus(
            LocalDate installmentDate,
            LocalDate removeDate,
            Long id,
            Long gridOperatorId,
            MeterStatus status
    );


    @Query(
            value = """
                    select count(m.id) > 0 from Meter m
                    join BillingDataByScale bdbs on bdbs.meterId = m.id
                    join BillingByScale bbs on bdbs.billingByScaleId = bbs.id
                        where m.id = :id
                        and m.status = 'ACTIVE'
                        and bbs.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToBillingByScale(@Param("id") Long id);

    @Query("""
            SELECT m FROM Meter m
            WHERE m.number = :number
                AND :periodFrom BETWEEN m.installmentDate AND COALESCE(m.removeDate, :periodFrom)
                AND :periodTo BETWEEN m.installmentDate AND COALESCE(m.removeDate, :periodTo)
                AND m.status = :status
            """)
    Optional<Meter> findByNumberAndStatus(LocalDate periodFrom, LocalDate periodTo, String number, MeterStatus status);
}
