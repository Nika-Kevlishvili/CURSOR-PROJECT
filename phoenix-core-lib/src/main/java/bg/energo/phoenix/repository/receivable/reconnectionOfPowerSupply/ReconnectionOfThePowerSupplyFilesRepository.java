package bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyFiles;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ReconnectionOfThePowerSupplyFilesRepository extends FileExpiration<ReconnectionOfThePowerSupplyFiles>, JpaRepository<ReconnectionOfThePowerSupplyFiles, Long> {

    @Query("""
                    select rpsf from ReconnectionOfThePowerSupplyFiles rpsf
                    where rpsf.id in :ids
                    and rpsf.status in :statuses
            """)
    Set<ReconnectionOfThePowerSupplyFiles> findByIdsAndStatuses(Collection<Long> ids, List<EntityStatus> statuses);

    Optional<ReconnectionOfThePowerSupplyFiles> findByIdAndStatus(Long id, EntityStatus status);

    @Query("""
                    select rpsf from ReconnectionOfThePowerSupplyFiles rpsf
                    where rpsf.powerSupplyReconnectionId=:reconnectionId
                    and rpsf.status=:status
            """)
    Set<ReconnectionOfThePowerSupplyFiles> findByReconnectionIdAndSubObjectStatus(Long reconnectionId, EntityStatus status);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT rf.*
             FROM receivable.power_supply_reconnection_files AS rf
                      CROSS JOIN latest_doc_period dp
             WHERE rf.file_url IS NOT NULL
               AND rf.is_archived
               AND rf.status <> 'DELETED'
               AND CURRENT_DATE > ((rf.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<ReconnectionOfThePowerSupplyFiles> findExpiredFiles();

    @Query("""
            select count(rpsf.id) > 0
            from ReconnectionOfThePowerSupplyFiles rpsf
            where rpsf.id <> :currentEntityId
            and rpsf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            select psrf.id + 1
            from receivable.power_supply_reconnection_files psrf
            order by psrf.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

    @Query(value = """
            SELECT file.*
            FROM receivable.power_supply_reconnection_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<ReconnectionOfThePowerSupplyFiles> findFailedArchivationFiles();

}
