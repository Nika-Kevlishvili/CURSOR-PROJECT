package bg.energo.phoenix.repository.billing.companyDetails;

import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyDetails;
import bg.energo.phoenix.model.response.billing.CompanyDetailVersionsResponse;
import bg.energo.phoenix.service.billing.invoice.models.persistance.extractor.CompanyDetailProjection;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyDetailRepository extends JpaRepository<CompanyDetails, Long> {

    Optional<CompanyDetails> findByVersionId(Long versionId);


    @Query(value = "SELECT max(c.versionId) FROM CompanyDetails c")
    Optional<Long> findMaxVersionId();

    @Query(value = "SELECT c.name FROM CompanyDetails c WHERE c.versionId = :versionId")
    Optional<String> findCompanyDetailName(@Param("versionId") Long versionId);

    Optional<CompanyDetails> findCompanyDetailsByVersionId(Long versionId);

    @Query(value = """
            select c.id    as id,
                   cd.name as display_name
            from company.company_details cd
                     join company.companies c on cd.company_id = c.id
            where cd.start_date < current_date
            order by cd.start_date desc
            limit 1
            """, nativeQuery = true)
    Optional<CompanyDetailProjection> findLatestCompanyDetailVersion();

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.billing.CompanyDetailVersionsResponse(
                        cd.versionId,
                        cd.startDate
                    ) 
                    from CompanyDetails cd
                    """)
    List<CompanyDetailVersionsResponse> getVersions();

    Optional<CompanyDetails> findByStartDate(LocalDate versionStartDate);

    @Query(value = """
                  with current_company_details as (select cd.id,
                                                          cd.identifier,
                                                          cd.vat_number,
                                                          cd.number_under_excise_duties_tax_wh_act,
                                                          cd.name,
                                                          cd.name_transl,
                                                          cd.management_address,
                                                          cd.management_address_transl
                                             from company.company_details cd
                                             where cd.start_date <= :billingDate
                                             order by cd.start_date desc
                                             limit 1),
                 company_communication_addresses as (select cca.address,
                                                            cca.address_transl
                                                     from company.company_communication_addresses cca
                                                     where cca.company_detail_id = (select ccd.id from current_company_details ccd)
                                                       and cca.status = 'ACTIVE'),
                 company_managers as (select cm.manager,
                                             cm.manager_transl
                                      from company.company_managers cm
                                      where cm.company_detail_id = (select ccd.id from current_company_details ccd)
                                        and cm.status = 'ACTIVE'),
                 company_emails as (select ce.email
                                    from company.company_emails ce
                                    where ce.company_detail_id = (select ccd.id from current_company_details ccd)
                                      and ce.status = 'ACTIVE'),
                 company_phones as (select ct.telephone
                                    from company.company_telephones ct
                                    where ct.company_detail_id = (select ccd.id from current_company_details ccd)
                                      and ct.status = 'ACTIVE'),
                 company_banks as (select b.name, b.bic, cb.iban
                                   from company.company_banks cb
                                            join nomenclature.banks b on cb.bank_id = b.id
                                   where cb.company_detail_id = (select ccd.id from current_company_details ccd)
                                     and cb.status = 'ACTIVE'),
                 company_issuing_places as (select ciip.invoice_issue_place, ciip.invoice_issue_place_transl
                                            from company.company_invoice_issue_places ciip
                                            where ciip.company_detail_id = (select ccd.id from current_company_details ccd)
                                              and ciip.status = 'ACTIVE'),
                 company_compilers as (select cip.invoice_compiler, cip.invoice_compiler_transl
                                       from company.company_invoice_compilers cip
                                       where cip.company_detail_id = (select ccd.id from current_company_details ccd)
                                         and status = 'ACTIVE')
            select cd.id                                                                           as CompanyDetailId,
                   cd.identifier                                                                   as CompanyUIC,
                   cd.vat_number                                                                   as CompanyVATNumber,
                   cd.number_under_excise_duties_tax_wh_act                                        as CompanyExciseNumber,
                   cd.name                                                                         as CompanyName,
                   cd.name_transl                                                                  as CompanyNameTrsl,
                   cd.management_address                                                           as CompanyHeadquarterAddress,
                   cd.management_address_transl                                                    as CompanyHeadquarterAddressTrsl,
                   (select array_agg(cca.address) from company_communication_addresses cca)        as CompanyAddressList,
                   (select array_agg(cca.address_transl) from company_communication_addresses cca) as CompanyAddressTrslList,
                   (select array_agg(cm.manager) from company_managers cm)                         as CompanyManagerList,
                   (select array_agg(cm.manager_transl) from company_managers cm)                  as CompanyManagerTrslList,
                   (select array_agg(ce.email) from company_emails ce)                             as CompanyEmailList,
                   (select array_agg(cp.telephone) from company_phones cp)                         as CompanyPhoneList,
                   (select array_agg(cb.name) from company_banks cb)                               as CompanyBankList,
                   (select array_agg(cb.bic) from company_banks cb)                                as CompanyBicList,
                   (select array_agg(cb.iban) from company_banks cb)                               as CompanyIbanList,
                   (select array_agg(cip.invoice_issue_place) from company_issuing_places cip)     as CompanyIssuingPlaceList,
                   (select array_agg(cip.invoice_issue_place_transl)
                    from company_issuing_places cip)                                               as CompanyIssuingPlaceTrslList,
                   (select array_agg(cc.invoice_compiler) from company_compilers cc)               as CompanyComplierList,
                   (select array_agg(cc.invoice_compiler_transl) from company_compilers cc)        as CompanyComplierTrslList
            from current_company_details cd
            """, nativeQuery = true)
//    @Cacheable(value = "companyDetailsForTemplateCache")
    CompanyDetailedInformationModel getCompanyDetailedInformation(LocalDate billingDate);
}
