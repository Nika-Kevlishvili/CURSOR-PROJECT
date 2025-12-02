package bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToCbgDocFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ObjectionWithdrawalToCbgDocFileRepository extends JpaRepository<ObjectionWithdrawalToCbgDocFile, Long> {

    @Query(value = """
            select witdrawalfiles.id + 1
                          from receivable.objection_withdrawal_to_change_of_cbg_doc_files witdrawalfiles
                          order by witdrawalfiles.id desc
                          limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

}
