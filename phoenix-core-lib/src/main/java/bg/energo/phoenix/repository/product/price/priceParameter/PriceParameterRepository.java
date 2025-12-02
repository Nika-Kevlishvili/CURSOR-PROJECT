package bg.energo.phoenix.repository.product.price.priceParameter;

import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameter;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyPriceParameterEvaluationResult;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterForCalculationResponse;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterListingResponse;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterPreviewResponse;
import bg.energo.phoenix.service.billing.runs.models.PriceParameterRangeModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceParameterRepository extends JpaRepository<PriceParameter, Long> {

    @Query("""
                select pp
                from PriceParameter as pp
                where pp.id =:id
                and pp.status =:status
            """)
    Optional<PriceParameter> findByIdAndStatus(
            @Param("id") Long id,
            @Param("status") PriceParameterStatus status
    );


    @Query("""
                select new bg.energo.phoenix.model.response.priceParameter.PriceParameterPreviewResponse(
                    pp.id,
                    ppd.name,
                    concat(ppd.versionId, ' / ', ppd.name),
                    ppd.versionId,
                    pp.periodType,
                    pp.status,
                    pp.timeZone
                )
                from PriceParameter as pp
                join PriceParameterDetails ppd on pp.id = ppd.priceParameterId
                where pp.id =:id
                and ppd.versionId = coalesce(:version, (select max(maxDetailVersion.versionId) from PriceParameterDetails maxDetailVersion where maxDetailVersion.priceParameterId = :id))
                and pp.status in(:status)
            """)
    Optional<PriceParameterPreviewResponse> findByIdVersionAndStatusMapToPreviewResponse(
            @Param("id") Long id,
            @Param("version") Long version,
            @Param("status") List<PriceParameterStatus> status
    );


    @Query("""
            select new bg.energo.phoenix.model.response.priceParameter.PriceParameterListingResponse(
                p.id,
                pd2.name,
                p.status,
                p.periodType,
                p.createDate
            )
            from PriceParameter p
            join PriceParameterDetails pd2 on pd2.id = p.lastPriceParameterDetailId
                where p.id in (
                    select p2.id
                    from PriceParameter p2
                    join PriceParameterDetails pd on pd.priceParameterId = p2.id
                        where (
                            :columnname is null or (:columnname = 'ALL' and (lower(pd.name) like :columnvalue))
                            or (:columnname = 'NAME' and (lower(pd.name) like :columnvalue))
                        )
                        and (coalesce(:priceparametertype,'0') = '0' or p.periodType in :priceparametertype)
                        and (coalesce(:excludeOldVersions,'false') = 'false' or (:excludeOldVersions = 'true' and pd.id = p.lastPriceParameterDetailId)))
                        and p.status in (:statuses)
            """)
    Page<PriceParameterListingResponse> filter(
            @Param("columnvalue") String prompt,
            @Param("columnname") String searchField,
            @Param("priceparametertype") List<PeriodType> priceParameterTypes,
            @Param("statuses") List<PriceParameterStatus> statuses,
            @Param("excludeOldVersions") String excludeOldVersions,
            Pageable pageable
    );


    @Query(
            nativeQuery = true,
            value = """
                    select
                        id,
                        avg_price as averagePrice,
                        cnt as count,
                        period_type as periodType
                    from (
                        select
                            pp.id,
                            pp.period_type,
                            (select count(1) from(
                                    select ppdi.price,
                                    extract(month from ppdi.period_from at time zone 'utc') as month_number,
                                    extract(hours from ppdi.period_from at time zone 'utc') as hours,
                                    ppdi.is_shifted_hour,
                                    ppdi.period_from at time zone 'utc' as period_from
                                        from prices.price_parameter_detail_info ppdi
                                        where ppdi.price_parameter_detail_id = ppd.id
                                        and case when pp.period_type in ('FIFTEEN_MINUTES','ONE_HOUR','ONE_DAY')
                                                 then date(period_from at time zone 'utc') > date(:executionDate) - 31 and date(period_from at time zone 'utc') < :executionDate
                                                 when pp.period_type = 'ONE_MONTH'
                                                 then ppdi.period_from at time zone 'utc' = date_trunc('month', date(:executionDate) - interval '1' month)
                                                 end
                                ) as tbl
                                    where
                                    not(
                                        pp.period_type in ('FIFTEEN_MINUTES', 'ONE_HOUR')
                                            and tbl.month_number = 3
                                            and date(tbl.period_from at time zone 'utc') = :lastSundayInMarch
                                            and pp.time_zone = 'CET'
                                            and tbl.hours = 3
                                    )
                                    and not(
                                        pp.period_type in ('FIFTEEN_MINUTES', 'ONE_HOUR')
                                           and tbl.month_number = 3
                                           and date(tbl.period_from at time zone 'utc') = :lastSundayInMarch
                                           and pp.time_zone = 'EET'
                                           and tbl.hours = 4
                                    )
                                    and
                                        case when pp.period_type in ('FIFTEEN_MINUTES', 'ONE_HOUR')
                                           and tbl.month_number = 10
                                           and date(tbl.period_from at time zone 'utc') = :lastSundayInOctober
                                           and pp.time_zone = 'CET'
                                           and tbl.hours = 3 then is_shifted_hour in (true, false)
                                        when pp.period_type in ('FIFTEEN_MINUTES', 'ONE_HOUR')
                                           and tbl.month_number = 10
                                           and date(tbl.period_from at time zone 'utc')  = :lastSundayInOctober
                                           and pp.time_zone = 'EET'
                                           and tbl.hours = 4 then is_shifted_hour in (true, false)
                                        else is_shifted_hour = false end
                          ) as cnt,
                          (select round(avg(tbl.price),10) from(
                                select
                                    ppdi.price,
                                    extract(month from ppdi.period_from at time zone 'utc') as month_number,
                                    extract(hours from ppdi.period_from at time zone 'utc') as hours,
                                    ppdi.is_shifted_hour,
                                    ppdi.period_from at time zone 'utc' as period_from
                                from prices.price_parameter_detail_info ppdi
                                    where ppdi.price_parameter_detail_id = ppd.id
                                        and case when pp.period_type in ('FIFTEEN_MINUTES','ONE_HOUR','ONE_DAY')
                                                 then date(ppdi.period_from at time zone 'utc') > date(:executionDate) - 31 and date(period_from at time zone 'utc') < :executionDate
                                                 when pp.period_type = 'ONE_MONTH'
                                                 then ppdi.period_from at time zone 'utc' = date_trunc('month', date(:executionDate) - interval '1' month)
                                                 end
                               ) as tbl
                                    where
                                    not(
                                        pp.period_type in ('FIFTEEN_MINUTES', 'ONE_HOUR')
                                            and tbl.month_number = 3
                                            and date(tbl.period_from at time zone 'utc') = :lastSundayInMarch
                                            and pp.time_zone = 'CET'
                                            and tbl.hours = 3
                                    )
                                    and not(
                                        pp.period_type in ('FIFTEEN_MINUTES', 'ONE_HOUR')
                                           and tbl.month_number = 3
                                           and date(tbl.period_from at time zone 'utc') = :lastSundayInMarch
                                           and pp.time_zone = 'EET'
                                           and tbl.hours = 4
                                    )
                                    and
                                        case when pp.period_type in ('FIFTEEN_MINUTES', 'ONE_HOUR')
                                           and tbl.month_number = 10
                                           and date(tbl.period_from at time zone 'utc') = :lastSundayInOctober
                                           and pp.time_zone = 'CET'
                                           and tbl.hours = 3 then is_shifted_hour in (true, false)
                                        when pp.period_type in ('FIFTEEN_MINUTES', 'ONE_HOUR')
                                           and tbl.month_number = 10
                                           and date(tbl.period_from at time zone 'utc')  = :lastSundayInOctober
                                           and pp.time_zone = 'EET'
                                           and tbl.hours = 4 then is_shifted_hour in (true, false)
                                        else is_shifted_hour = false end
                          ) as avg_price
                        from prices.price_parameters pp
                        join prices.price_parameter_details ppd on pp.last_price_parameter_detail_id = ppd.id
                            and ppd.price_parameter_id = pp.id
                            and pp.status = 'ACTIVE'
                            and pp.id in :priceParameterIds
                    ) as tbl
                        where
                            case when period_type = 'FIFTEEN_MINUTES'
                                then cnt = 2880 + case
                                    when (:lastSundayInOctober between date(:executionDate) - 31 and :executionDate) then 4
                                    when (:lastSundayInMarch between date(:executionDate) - 31 and :executionDate) then -4
                                    else 0 end
                            when period_type = 'ONE_HOUR'
                                then cnt = 720 + case
                                    when (:lastSundayInOctober between date(:executionDate) - 31 and :executionDate) then 1
                                    when (:lastSundayInMarch between date(:executionDate) - 31 and :executionDate) then -1
                                    else 0 end
                            when period_type = 'ONE_DAY' then cnt = 30
                            when period_type = 'ONE_MONTH' then cnt = 1 end;
                    """
    )
    List<ActionPenaltyPriceParameterEvaluationResult> findAveragePriceOver30DaysAndPriceParameterIdIn(
            @Param("executionDate") LocalDate executionDate,
            @Param("priceParameterIds") List<Long> priceParameterIds,
            @Param("lastSundayInOctober") LocalDate lastSundayInOctober,
            @Param("lastSundayInMarch") LocalDate lastSundayInMarch
    );


    @Query("""
            select pp from PriceParameter pp
            join PriceParameterDetails ppd on ppd.id=pp.lastPriceParameterDetailId
            join MeasurementType mt on mt.name =ppd.name
            join PointOfDeliveryDetails pdt on pdt.podMeasurementTypeId =mt.id
            where pdt.id=:podDetailId
            and pp.status='ACTIVE'
            """)
    Optional<PriceParameter> findParameterIdWithPodDetailAndValuesFilled(Long podDetailId/*, LocalDate startDate, LocalDate endDate*/);

    @Query(value = """
                   SELECT date_range as periodfrom, tbl.period_to as periodTo, tbl.price as price,tbl.is_shifted_hour as isShiftedHour
                   FROM generate_series(:fromDate, :toDate, 
                   case 
                   when :periodType = '15 minute' then interval '15 minute'
                   when :periodType = '1 hour' then interval '1 hour'
                   when :periodType = '1 day' then interval '1 day'
                   when :periodType = '1 month' then interval '1 month'
                   end
                   ) AS date_range
                           left join (select ppdi.id, ppdi.period_from, ppdi.period_to,ppdi.price,ppdi.is_shifted_hour
                                  from prices.price_parameter_detail_info ppdi
                            left join prices.price_parameter_details pdd
                                  on ppdi.price_parameter_detail_id = pdd.id
                                      left join prices.price_parameters pp on pp.last_price_parameter_detail_id=pdd.id
                                  where pp.id=:priceParameterId ) as tbl on tbl.period_from=date_range
            """, nativeQuery = true)
    List<PriceParameterRangeModel> findDetailInfoInGivenPeriod(@Param("priceParameterId")Long priceParameterId,
                                                               @Param("fromDate")LocalDateTime fromDate,
                                                               @Param("toDate")LocalDateTime toDate,
                                                               @Param("periodType")String periodType);
    @Query(nativeQuery = true,
    value = """
            with last_sundays as (select cast(date_trunc('year', current_date) as date) + interval '3 month' - interval '1 day' -
                                         interval '1 day' * extract(dow from cast(date_trunc('year', current_date) as date) +
                                                                             interval '3 month' - interval '1 day')
                                             as last_sunday_in_march,
                                         cast(date_trunc('year', current_date) as date) + interval '10 month' - interval '1 day' -
                                         interval '1 day' * extract(dow from cast(date_trunc('year', current_date) as date) +
                                                                             interval '10 month' - interval '1 day')
                                             as last_sunday_in_october)
            select case
                       when pp.period_type = 'FIFTEEN_MINUTES' then (
                           case
                               when (pp.time_zone = 'CET' and
                                     (last_sundays.last_sunday_in_march >= cast((current_date - interval '30 days') as date)
                                         and last_sundays.last_sunday_in_march <= cast((current_date - interval '1 days') as date)))
                                   then (count(case when ppdi.is_shifted_hour = false then 1 end) = 2880
                                   and count(case
                                                 when ppdi.is_shifted_hour = true and
                                                      date(ppdi.period_from) = date(last_sundays.last_sunday_in_march)
                                                     then 1 end) = 4)
                               when (pp.time_zone = 'EET' and
                                     (last_sundays.last_sunday_in_october >= cast((current_date - interval '30 days') as date)
                                         and
                                      last_sundays.last_sunday_in_october <= cast((current_date - interval '1 days') as date)))
                                   then (count(case when ppdi.is_shifted_hour = false then 1 end) = 2880
                                   and count(case
                                                 when ppdi.is_shifted_hour = true and
                                                      date(ppdi.period_from) = date(last_sundays.last_sunday_in_october)
                                                     then 1 end) = 4)
                               else count(case when ppdi.is_shifted_hour = false then 1 end) = 2880
                               end)
                       when pp.period_type = 'ONE_HOUR' then (
                           case
                               when (pp.time_zone = 'CET' and
                                     (last_sundays.last_sunday_in_march >= cast((current_date - interval '30 days') as date)
                                         and last_sundays.last_sunday_in_march <= cast((current_date - interval '1 days') as date)))
                                   then (count(case when ppdi.is_shifted_hour = false then 1 end) = 720
                                   and count(case
                                                 when ppdi.is_shifted_hour = true and
                                                      date(ppdi.period_from) = date(last_sundays.last_sunday_in_march) then 1 end) =
                                       1)
                               when (pp.time_zone = 'EET' and
                                     (last_sundays.last_sunday_in_october >= cast((current_date - interval '30 days') as date)
                                         and
                                      last_sundays.last_sunday_in_october <= cast((current_date - interval '1 days') as date)))
                                   then (count(case when ppdi.is_shifted_hour = false then 1 end) = 720
                                   and count(case
                                                 when ppdi.is_shifted_hour = true and
                                                      date(ppdi.period_from) = date(last_sundays.last_sunday_in_october)
                                                     then 1 end) = 1)
                               else count(case when ppdi.is_shifted_hour = false then 1 end) = 720
                               end)
                       when pp.period_type = 'ONE_DAY' then
                           count(ppdi.id) = 30
                       else count(ppdi.id) = 1
                       end                          as previousThirtyDaysFilled,
                   count(distinct ppdi.period_from) as totalPeriodsWithinRange,
                   sum(ppdi.price)                  as totalPrice,
                   pp.id                            as id
            from prices.price_parameters pp
                     join prices.price_parameter_details ppd
                          on pp.last_price_parameter_detail_id = ppd.id and pp.id = ppd.price_parameter_id
                     left join prices.price_parameter_detail_info ppdi on ppd.id = ppdi.price_parameter_detail_id
                     cross join last_sundays
            where pp.status = 'ACTIVE'
              and pp.id in (:priceParameterIds)
              and (
                (pp.period_type in ('FIFTEEN_MINUTES', 'ONE_HOUR', 'ONE_DAY') and
                 ppdi.period_from >= cast((current_date - interval '30 days') as date) + interval '00:00:00' and
                 ppdi.period_from <= cast((current_date - interval '1 days') as date) + interval '23:45:00')
                    or (pp.period_type = 'ONE_MONTH' and
                        extract(month from ppdi.period_from) = extract(month from current_date - interval '1 month') and
                        extract(year from ppdi.period_from) = extract(year from current_date - interval '1 month'))
                )
            group by pp.period_type, pp.time_zone, last_sundays.last_sunday_in_october, last_sundays.last_sunday_in_march, pp.id
                       """)
    List<PriceParameterForCalculationResponse> isPreviousThirtyDaysFilled(@Param("priceParameterIds") List<Long> priceParameterIds);

}
