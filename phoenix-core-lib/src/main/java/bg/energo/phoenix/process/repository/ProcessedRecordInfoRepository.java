package bg.energo.phoenix.process.repository;

import bg.energo.phoenix.process.model.entity.ProcessedRecordInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface ProcessedRecordInfoRepository extends JpaRepository<ProcessedRecordInfo, Long> {

    Optional<ProcessedRecordInfo> findFirstByProcessIdOrderByRecordIdDesc(Long processId);

    List<ProcessedRecordInfo> findAllByProcessIdAndSuccessOrderByRecordIdAsc(Long processId, Boolean success);

    Optional<ProcessedRecordInfo> findFirstByProcessIdAndSuccess(Long processId, Boolean success);

    @Query(
            """
                    select pri from ProcessedRecordInfo as pri
                        where pri.processId = :processId
                        and pri.success = :success
                        order by pri.createDate asc
                    """
    )
    List<ProcessedRecordInfo> fetchProcessedRecordInfosInRangeByProcessId(
            @Param("processId") Long processId,
            @Param("success") Boolean success,
            Pageable pageable
    );

    @Query("""
                 select pri from ProcessedRecordInfo  as pri
                 where pri.id in :value
            """)
    Stream<ProcessedRecordInfo> findProcessInfoStreamByIds(List<Long> value);


    @Query("""
                update ProcessedRecordInfo pri
                set pri.success = false, pri.errorMessage = :message
                where pri.processId = :processId
            """)
    @Modifying
    void updateAllByProcessId(Long processId,String message);

    boolean existsByProcessIdAndSuccess(Long processId, Boolean success);
}
