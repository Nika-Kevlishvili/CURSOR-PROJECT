package bg.energo.phoenix.repository.receivable.reminder;

import bg.energo.phoenix.model.entity.receivable.reminder.ReminderLetterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderLetterTemplateRepository extends JpaRepository<ReminderLetterTemplate, Long> {

}
