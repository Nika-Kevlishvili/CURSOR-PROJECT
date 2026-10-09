package bg.energo.phoenix.cashterminal.repository;

import bg.energo.phoenix.cashterminal.model.entity.CashTerminalVerifyDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashTerminalVerifyDetailsRepository extends JpaRepository<CashTerminalVerifyDetailsEntity, Long> {
}

