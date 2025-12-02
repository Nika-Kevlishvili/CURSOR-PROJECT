package bg.energo.phoenix.repository.pod.billingByProfile;

import bg.energo.phoenix.billingRun.model.BillingDataByProfilePricePrice;
import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingDataByProfile;
import bg.energo.phoenix.model.response.pod.billingByProfile.BillingByProfileDataResponse;
import bg.energo.phoenix.service.billing.runs.models.BillingDatesModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingDataByProfileRepository extends JpaRepository<BillingDataByProfile, Long> {
    @Query("""
                select ppdi
                from BillingDataByProfile as ppdi
                where ppdi.billingByProfileId = :billingByProfileId
                and :periodFrom = ppdi.periodFrom
                and :periodTo = ppdi.periodTo
                and ppdi.isShiftedHour =:isShiftedHour
            """)
    Optional<BillingDataByProfile> findByBillingDataIdAndPeriodAndIsShiftedHour(
            @Param("billingByProfileId") Long billingByProfileId,
            @Param("periodFrom") LocalDateTime periodFrom,
            @Param("periodTo") LocalDateTime periodTo,
            @Param("isShiftedHour") Boolean isShiftedHour
    );

    @Query(value = """
                
                WITH input_data AS (
                    SELECT * FROM jsonb_to_recordset(
                        cast (:jsonData as jsonb)
                    ) AS x(f TIMESTAMP without time zone, t TIMESTAMP without time zone, v NUMERIC, s BOOLEAN)
                )
                INSERT INTO pod.billing_data_by_profile
                (period_from, period_to, value, is_shifted_hour, create_date, system_user_id, modify_date, modify_system_user_id, billing_by_profile_id)
                SELECT src.f, src.t, src.v, src.s, now(), :systemUserId, now(), :systemUserId, :billingDataProfileId
                FROM input_data AS src
                ON CONFLICT (period_from, is_shifted_hour, billing_by_profile_id)
                DO UPDATE 
                SET value=EXCLUDED.value,is_shifted_hour=EXCLUDED.is_shifted_hour,modify_date=now(), modify_system_user_id=:systemUserId
            """, nativeQuery = true)
    @Modifying
    void insertOrUpdateBatch(@Param("billingDataProfileId") Long billingDataProfileId, @Param("systemUserId") String systemUserId, @Param("jsonData") String jsonData);

    @Query(value = """
                        delete from BillingDataByProfile b where b.billingByProfileId =:billingByProfileId and b.periodFrom in(:periodFrom)
            """)
    @Modifying
    void deleteAllNotPassedValues(@Param("billingByProfileId") Long billingByProfileId, @Param("periodFrom") List<LocalDateTime> periodFrom);

    List<BillingDataByProfile> findByBillingByProfileId(Long id);

    @Query(nativeQuery = true, value = """
        select *
        from pod.billing_data_by_profile bdbp
        where bdbp.billing_by_profile_id = :profileId
          and bdbp.period_from >= :periodFrom
          and bdbp.period_from <= :periodTo
        order by bdbp.period_from asc
    """)
    List<BillingDataByProfile> fetchBillingDataWithinPeriod(Long profileId, LocalDateTime periodFrom, LocalDateTime periodTo);

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.pod.billingByProfile.BillingByProfileDataResponse(
                        bdbp
                    )
                    from BillingDataByProfile bdbp
                    join BillingByProfile bbp on bbp.id = bdbp.billingByProfileId
                        where bdbp.periodFrom >= :periodFrom
                        and bdbp.periodFrom <= :periodTo
                        and bdbp.billingByProfileId = :billingByProfileId
                    order by bdbp.periodFrom asc
                    """
    )
    List<BillingByProfileDataResponse> findResponseByBillingByProfileIdAndPeriodFromAndPeriodTo(
            @Param("billingByProfileId") Long billingByProfileId,
            @Param("periodFrom") LocalDateTime periodFrom,
            @Param("periodTo") LocalDateTime periodTo
    );

    @Query(
            value = """
                    select bdbp
                    from BillingDataByProfile bdbp
                    join BillingByProfile bbp on bbp.id = bdbp.billingByProfileId
                        where bdbp.periodFrom >= :periodFrom
                        and bdbp.periodTo <= :periodTo
                        and bdbp.billingByProfileId = :billingByProfileId
                    order by bdbp.periodFrom asc
                    """
    )
    List<BillingDatesModel> findByBillingByProfileIdAndPeriodFromAndPeriodTo(
            @Param("billingByProfileId") Long billingByProfileId,
            @Param("periodFrom") LocalDateTime periodFrom,
            @Param("periodTo") LocalDateTime periodTo
    );

    @Query(
            value = """
                    select  DISTINCT bdbp.value from pod.billing_data_by_profile bdbp 
                    inner join billing_run.bdp_periods_pp r on r.id  = bdbp.billing_by_profile_id 
                    where bdbp.period_from =:date
                    	 and CAST(r.period_type AS VARCHAR) =:periodType
                    	 and r.pod_id  =:podId
                    	 and r.run_id  =:runId
                    """, nativeQuery = true
    )
    BillingDataByProfilePricePrice findPriceProfileByPodIdForExactDateWithType(
            @Param("runId") Long runId,
            @Param("podId") Long podId,
            @Param("date") LocalDateTime date,
            @Param("periodType") String periodType

    );

    void deleteByBillingByProfileId(Long billingProfileId);
}
