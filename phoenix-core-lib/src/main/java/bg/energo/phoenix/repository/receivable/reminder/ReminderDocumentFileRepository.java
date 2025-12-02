package bg.energo.phoenix.repository.receivable.reminder;

import bg.energo.phoenix.model.entity.receivable.reminder.ReminderDocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderDocumentFileRepository extends JpaRepository<ReminderDocumentFile, Long> {

    Optional<List<ReminderDocumentFile>> findByReminderId(Long reminderId);

}
