package bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationFiles;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PowerSupplyDcnCancellationFileRepository extends FileExpiration<PowerSupplyDcnCancellationFiles>, JpaRepository<PowerSupplyDcnCancellationFiles, Long> {
    Optional<List<PowerSupplyDcnCancellationFiles>> findByPowerSupplyDcnCancellationIdAndStatus(Long reminderId, EntityStatus status);

    @Query("""
                    select psdc from PowerSupplyDcnCancellationFiles psdc
                    where psdc.id in :ids
                    and psdc.status in :statuses
            """)
    Set<PowerSupplyDcnCancellationFiles> findByIdsAndStatuses(Collection<Long> ids, List<EntityStatus> statuses);


    @Query("""
                    select psc from PowerSupplyDcnCancellationFiles psc
                    where psc.powerSupplyDcnCancellationId=:cancellationId
                    and psc.status=:status
            """)
    Set<PowerSupplyDcnCancellationFiles> findByCancellationIdAndSubObjectStatus(Long cancellationId, EntityStatus status);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT df.*
             FROM receivable.power_supply_dcn_cancellation_files AS df
                      CROSS JOIN latest_doc_period dp
             WHERE df.file_url IS NOT NULL
               AND df.is_archived
               AND df.status <> 'DELETED'
               AND CURRENT_DATE > ((df.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<PowerSupplyDcnCancellationFiles> findExpiredFiles();

    @Query("""
            select count(psdf.id) > 0
            from PowerSupplyDcnCancellationFiles psdf
            where psdf.id <> :currentEntityId
            and psdf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            SELECT file.*
            FROM receivable.power_supply_dcn_cancellation_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<PowerSupplyDcnCancellationFiles> findFailedArchivationFiles();

}
