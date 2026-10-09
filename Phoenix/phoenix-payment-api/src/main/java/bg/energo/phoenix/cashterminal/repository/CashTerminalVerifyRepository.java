package bg.energo.phoenix.cashterminal.repository;

import bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashTerminalVerifyRepository extends JpaRepository<CashTerminalVerifyEntity, Long> {
}

