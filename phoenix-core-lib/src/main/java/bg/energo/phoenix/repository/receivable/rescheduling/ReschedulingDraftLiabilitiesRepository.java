package bg.energo.phoenix.repository.receivable.rescheduling;

import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingDraftLiabilities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReschedulingDraftLiabilitiesRepository extends JpaRepository<ReschedulingDraftLiabilities,Long> {

    @Query("""
        select distinct r.customerLiabilityId from ReschedulingDraftLiabilities r
        where r.reschedulingId=:reschedulingId
""")
    List<Long> findCustomerLiabilityIdIdByReschedulingId(Long reschedulingId);

    @Query("""
        select r.id from  ReschedulingDraftLiabilities r
        where r.reschedulingId=:reschedulingId
""")
    List<Long> findByReschedulingId(Long reschedulingId);
}
