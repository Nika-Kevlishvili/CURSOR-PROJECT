package bg.energo.phoenix.repository.pod.billingByScales;

import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScale;
import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScaleStatus;
import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScalesMaxMinReading;
import bg.energo.phoenix.model.response.pod.billingByScales.BillingByScaleListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BillingByScaleRepository extends JpaRepository<BillingByScale, Long> {

    @Query("""
            select new bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScalesMaxMinReading(
                max(bdbs.oldMeterReading),
                min(bdbs.newMeterReading)
            )
            from BillingDataByScale bdbs
            join BillingByScale bbs on bdbs.billingByScaleId = bbs.id
                where bdbs.scaleId = :scaleId
                and bbs.podId = :podId
                and bdbs.meterId = :meterId
            """)
    BillingByScalesMaxMinReading findByPodIdAndScaleCodaAndMeter(
            @Param("scaleId") Long scaleId,
            @Param("podId") Long podId,
            @Param("meterId") Long meterId
    );


    @Query(value = """
            select new bg.energo.phoenix.model.response.pod.billingByScales.BillingByScaleListResponse(
                bbs.id,
                p.identifier as identifier,
                bbs.dateFrom,
                bbs.dateTo,
                case when bbs.invoiced then 'YES' else 'NO'end  as invoiced,
                bbs.status,
                bbs.createDate
            )
            from BillingByScale bbs
            join PointOfDelivery p on bbs.podId = p.id
                where bbs.status in (:status)
                and
                ((cast(:datefromfrom as date) is not null and cast(:datefromto as date) is not null  and bbs.dateFrom  between  :datefromfrom and :datefromto)
                or
                (cast(:datefromfrom as date) is not null and cast(:datefromto as date) is null and bbs.dateFrom  >=  :datefromfrom)
                or
                (cast(:datefromfrom as date) is null and cast(:datefromto as date) is not null  and bbs.dateFrom  <=  :datefromto)
                or
                (cast(:datefromfrom as date) is null and cast(:datefromto as date) is null)
                )
                and
                (
                (cast(:datetofrom as date) is not null and cast(:datetoto as date) is not null  and bbs.dateTo  between  :datetofrom and :datetoto)
                or
                (cast(:datetofrom as date) is not null and cast(:datetoto as date) is null  and bbs.dateTo  >= :datetofrom)
                or
                (cast(:datetofrom as date) is null and cast(:datetoto as date) is not null  and bbs.dateTo  <= :datetoto)
                or
                (cast(:datetofrom as date) is null and cast(:datetoto as date) is null)
                )
                and
                (coalesce(:invoiced,'0') = '0' or  case when bbs.invoiced then 'YES' else 'NO'end =:invoiced )
                and
                (:columnname is null or (:columnname =  'ALL' and ((cast(bbs.id as string ) like :columnvalue) or lower(p.identifier) like :columnvalue))
                                                              or ((:columnname = 'ID' and cast(bbs.id as string )  like :columnvalue))
                                                              or ((:columnname= 'POD_IDENTIFIER' and lower(p.identifier) like :columnvalue))
                )
            """
    )
    Page<BillingByScaleListResponse> filter(
            @Param("columnvalue") String prompt,
            @Param("columnname") String columnName,
            @Param("invoiced") String invoiced,
            @Param("datefromfrom") LocalDate dateFrom,
            @Param("datefromto") LocalDate dateFromTo,
            @Param("datetofrom") LocalDate dateToFrom,
            @Param("datetoto") LocalDate dateTo,
            @Param("status") List<BillingByScaleStatus> status,
            Pageable pageable
    );

    @Query("""
            select bbs.id from BillingByScale bbs
            where bbs.podId = :id AND bbs.status = 'ACTIVE' AND 
           ((bbs.dateFrom <= :dateTo AND bbs.dateTo >= :dateFrom)
                OR (bbs.dateFrom = :dateFrom AND bbs.dateTo = :dateTo))
            """)
    List<Long> findByPodIdAndDateFromAndDateTo(Long id, LocalDate dateFrom, LocalDate dateTo);
}
