package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.documentModels.contract.response.PodResponse;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractPods;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServiceContractPodsRepository extends JpaRepository<ServiceContractPods, Long> {
    List<ServiceContractPods> findByContractDetailIdAndStatus(Long id, ContractSubObjectStatus status);

    List<ServiceContractPods> findByContractDetailIdAndStatusAndIdNotIn(Long id, ContractSubObjectStatus status, List<Long> ids);

    Optional<ServiceContractPods> findByIdAndContractDetailIdAndStatus(Long id, Long contractId, ContractSubObjectStatus status);
    @Query(nativeQuery = true, value = """
            (with previous_version_pods as (select pod.identifier, cp.id
                                            from pod.pod pod
                                                     join service_contract.contract_pods cp
                                                          on pod.id = cp.pod_id and cp.status = 'ACTIVE'
                                                     join service_contract.contract_details cd
                                                          on cp.contract_detail_id = cd.id and cd.version_id = :versionId - 1
                                                     join service_contract.contracts c on cd.contract_id = c.id and c.id = :id),
                  current_version_pods as (select pod.identifier, cp.id
                                           from pod.pod pod
                                                    join service_contract.contract_pods cp
                                                         on pod.id = cp.pod_id and cp.status = 'ACTIVE'
                                                    join service_contract.contract_details cd
                                                         on cp.contract_detail_id = cd.id and cd.version_id = :versionId
                                                    join service_contract.contracts c on cd.contract_id = c.id and c.id = :id),
                  pods_all_info as (select cont_pd.id,
                                           pod.identifier,
                                           pd.additional_identifier,
                                           text(pd.type)                                   as type,
                                           replace(text(pd.consumption_purpose), '_', ' ') as consumption_purpose,
                                           replace(text(pd.measurement_type), '_', ' ')    as measurement_type,
                                           pd.estimated_monthly_avg_consumption,
                                           pd.name                                         as pd_name,
                                           go.name                                         as go_name,
                                           case
                                               when pd.foreign_address = true then pd.populated_place_foreign
                                               else pp.name end                            as populated_place,
                                           case
                                               when pd.foreign_address = true then pd.zip_code_foreign
                                               else zc.zip_code end                        as zip_code,
                                           case
                                               when pd.foreign_address = true then pd.district_foreign
                                               else distr.name end                         as district,
                                           case
                                               when pd.foreign_address = true
                                                   then replace(text(pd.foreign_residential_area_type), '_', ' ')
                                               else replace(text(ra.type), '_', ' ') end   as ra_type,
                                           case
                                               when pd.foreign_address = true then pd.residential_area_foreign
                                               else ra.name end                            as ra_name,
                                           case
                                               when pd.foreign_address = true then text(pd.foreign_street_type)
                                               else text(str.type) end                     as street_type,
                                           case
                                               when pd.foreign_address = true then pd.street_foreign
                                               else str.name end                           as street,
                                           pd.street_number,
                                           pd.block,
                                           pd.entrance,
                                           pd.floor,
                                           pd.apartment,
                                           pd.address_additional_info,
                                           case
                                               when pd.foreign_address = false then
                                                   concat_ws(', ',
                                                             nullif(distr.name, ''),
                                                             nullif(concat_ws(' ', replace(text(ra.type), '_', ' '), ra.name), ''),
                                                             nullif(concat_ws(' ', str.type, str.name, pd.street_number), ''),
                                                             nullif(concat('бл. ', pd.block), 'бл. '),
                                                             nullif(concat('вх. ', pd.entrance), 'вх. '),
                                                             nullif(concat('ет. ', pd.floor), 'ет. '),
                                                             nullif(concat('ап. ', pd.apartment), 'ап. '),
                                                             pd.address_additional_info
                                                   )
                                               else
                                                   concat_ws(', ',
                                                             nullif(pd.district_foreign, ''),
                                                             nullif(concat_ws(' ',
                                                                              replace(text(pd.foreign_residential_area_type), '_', ' '),
                                                                              pd.residential_area_foreign), ''),
                                                             nullif(
                                                                     concat_ws(' ', pd.foreign_street_type, pd.street_foreign,
                                                                               pd.street_number),
                                                                     ''),
                                                             nullif(concat('бл. ', pd.block), 'бл. '),
                                                             nullif(concat('вх. ', pd.entrance), 'вх. '),
                                                             nullif(concat('ет. ', pd.floor), 'ет. '),
                                                             nullif(concat('ап. ', pd.apartment), 'ап. '),
                                                             pd.address_additional_info
                                                   )
                                               end                                         as formatted_address
                                    from pod.pod pod
                                             join pod.pod_details pd on pod.last_pod_detail_id = pd.id
                                             join service_contract.contract_pods cont_pd
                                                  on pod.id = cont_pd.pod_id and cont_pd.status = 'ACTIVE'
                                             left join nomenclature.districts distr on pd.district_id = distr.id
                                             left join nomenclature.zip_codes zc on pd.zip_code_id = zc.id
                                             left join nomenclature.residential_areas ra on pd.residential_area_id = ra.id
                                             left join nomenclature.streets str on pd.street_id = str.id
                                             left join nomenclature.populated_places pp on pd.populated_place_id = pp.id
                                             join nomenclature.grid_operators go on pod.grid_operator_id = go.id)
             select cp.identifier                                                       as PODID,
                    cp.additional_identifier                                            as PODAdditionalID,
                    cp.pd_name                                                          as PODName,
                    translation.translate_text(cp.formatted_address, text('BULGARIAN')) as PODAddressComb,
                    cp.formatted_address                                                as PODAddressCombTrsl,
                    cp.populated_place                                                  as PODPlace,
                    cp.zip_code                                                         as PODZIP,
                    cp.type                                                             as PODType,
                    cp.go_name                                                          as PODGO,
                    cp.consumption_purpose                                              as PODConsumptionPurpose,
                    cp.measurement_type                                                 as PODMeasurementType,
                    cp.estimated_monthly_avg_consumption                                as EstimatedConsumption,
                    'ADDED'                                                             as PodState
             from current_version_pods p
                      join pods_all_info cp on p.id = cp.id
             where cp.identifier not in (select pvp.identifier from previous_version_pods pvp)
               and :versionId > 1
             union
             select cp.identifier                                                       as PODID,
                    cp.additional_identifier                                            as PODAdditionalID,
                    cp.pd_name                                                          as PODName,
                    translation.translate_text(cp.formatted_address, text('BULGARIAN')) as PODAddressComb,
                    cp.formatted_address                                                as PODAddressCombTrsl,
                    cp.populated_place                                                  as PODPlace,
                    cp.zip_code                                                         as PODZIP,
                    cp.type                                                             as PODType,
                    cp.go_name                                                          as PODGO,
                    cp.consumption_purpose                                              as PODConsumptionPurpose,
                    cp.measurement_type                                                 as PODMeasurementType,
                    cp.estimated_monthly_avg_consumption                                as EstimatedConsumption,
                    'REMOVED'                                                           as PodState
             from previous_version_pods p
                      join pods_all_info cp on p.id = cp.id
             where cp.identifier not in (select pvp.identifier from current_version_pods pvp)
             union
             select cp.identifier                                                       as PODID,
                    cp.additional_identifier                                            as PODAdditionalID,
                    cp.pd_name                                                          as PODName,
                    translation.translate_text(cp.formatted_address, text('BULGARIAN')) as PODAddressComb,
                    cp.formatted_address                                                as PODAddressCombTrsl,
                    cp.populated_place                                                  as PODPlace,
                    cp.zip_code                                                         as PODZIP,
                    cp.type                                                             as PODType,
                    cp.go_name                                                          as PODGO,
                    cp.consumption_purpose                                              as PODConsumptionPurpose,
                    cp.measurement_type                                                 as PODMeasurementType,
                    cp.estimated_monthly_avg_consumption                                as EstimatedConsumption,
                    'CURRENT'                                                           as PodState
             from current_version_pods p
                      join pods_all_info cp on p.id = cp.id)
            union
            (with previous_version_pods as (select cp.pod_identifier, cp.id
                                            from service_contract.contract_unrecognized_pods cp
                                                     join service_contract.contract_details cd
                                                          on cp.contract_detail_id = cd.id and cd.version_id = :versionId - 1
                                                     join service_contract.contracts c on cd.contract_id = c.id and c.id = :id
                                            where cp.status = 'ACTIVE'),
                  current_version_pods as (select cp.pod_identifier, cp.id
                                           from service_contract.contract_unrecognized_pods cp
                                                    join service_contract.contract_details cd
                                                         on cp.contract_detail_id = cd.id and cd.version_id = :versionId
                                                    join service_contract.contracts c on cd.contract_id = c.id and c.id = :id
                                           where cp.status = 'ACTIVE'),
                  pods_all_info as (select cont_pd.id,
                                           cont_pd.pod_identifier
                                    from service_contract.contract_unrecognized_pods cont_pd
                                    where cont_pd.status = 'ACTIVE')
             select cp.pod_identifier     as PODID,
                    text(null)            as PODAdditionalID,
                    text(null)            as PODName,
                    text(null)            as PODAddressComb,
                    text(null)            as PODAddressCombTrsl,
                    text(null)            as PODPlace,
                    text(null)            as PODZIP,
                    text(null)            as PODType,
                    text(null)            as PODGO,
                    text(null)            as PODConsumptionPurpose,
                    text(null)            as PODMeasurementType,
                    cast(null as numeric) as EstimatedConsumption,
                    'ADDED'               as PodState
             from current_version_pods p
                      join pods_all_info cp on p.id = cp.id
             where cp.pod_identifier not in (select pvp.pod_identifier from previous_version_pods pvp)
               and :versionId > 1
             union
             select cp.pod_identifier     as PODID,
                    text(null)            as PODAdditionalID,
                    text(null)            as PODName,
                    text(null)            as PODAddressComb,
                    text(null)            as PODAddressCombTrsl,
                    text(null)            as PODPlace,
                    text(null)            as PODZIP,
                    text(null)            as PODType,
                    text(null)            as PODGO,
                    text(null)            as PODConsumptionPurpose,
                    text(null)            as PODMeasurementType,
                    cast(null as numeric) as EstimatedConsumption,
                    'REMOVED'             as PodState
             from previous_version_pods p
                      join pods_all_info cp on p.id = cp.id
             where cp.pod_identifier not in (select pvp.pod_identifier from current_version_pods pvp)
             union
             select cp.pod_identifier     as PODID,
                    text(null)            as PODAdditionalID,
                    text(null)            as PODName,
                    text(null)            as PODAddressComb,
                    text(null)            as PODAddressCombTrsl,
                    text(null)            as PODPlace,
                    text(null)            as PODZIP,
                    text(null)            as PODType,
                    text(null)            as PODGO,
                    text(null)            as PODConsumptionPurpose,
                    text(null)            as PODMeasurementType,
                    cast(null as numeric) as EstimatedConsumption,
                    'CURRENT'             as PodState
             from current_version_pods p
                      join pods_all_info cp on p.id = cp.id)
            """)
    List<PodResponse> fetchVersionPodsForDocument(Long id, Long versionId);
}
