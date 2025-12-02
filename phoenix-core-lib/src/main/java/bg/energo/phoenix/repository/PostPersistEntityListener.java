package bg.energo.phoenix.repository;

import bg.energo.phoenix.model.entity.customer.Customer;
import jakarta.persistence.PostPersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PostPersistEntityListener {



    private static CustomerAuditRepository customerAuditRepository;


    public PostPersistEntityListener() {
    }

    @Autowired
    public void setMyService (CustomerAuditRepository customerAuditRepository) {
        this.customerAuditRepository=customerAuditRepository;
    }


    @PostPersist
    public void afterPersist(Customer object) {
        customerAuditRepository.save(object);
      log.info("Here I am : {}", object.toString());
    }


}
