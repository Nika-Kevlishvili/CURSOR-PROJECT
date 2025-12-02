package bg.energo.phoenix.repository.receivable.rescheduling;

import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingPlans;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReschedulingPlansRepository extends JpaRepository<ReschedulingPlans, Long> {

    List<ReschedulingPlans> findByReschedulingId(Long reschedulingId);
}
