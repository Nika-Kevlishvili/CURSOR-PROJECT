package bg.energo.phoenix.model.entity.contract.billing;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component

public class ContractPodEntityListener {
    @Value("${nomenclature.deactivation.purpose.newVersion.id}")
    private Long deactivationPurposeId;


    @PrePersist
    @PreUpdate
    public void beforeSave(ContractPods contractPods){
      if( contractPods.getDeactivationDate()!=null&& contractPods.getDeactivationPurposeId()==null){
          contractPods.setDeactivationPurposeId(deactivationPurposeId);
      }
      if(contractPods.getDeactivationDate()==null && contractPods.getDeactivationPurposeId()!=null){
          contractPods.setDeactivationPurposeId(null);
      }
    }
}
