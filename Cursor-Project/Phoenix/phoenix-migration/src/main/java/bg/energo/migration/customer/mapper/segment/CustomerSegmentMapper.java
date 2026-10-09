package bg.energo.migration.customer.mapper.segment;

import bg.energo.migration.customer.entity.segment.CustomerSegmentView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerSegmentMapper {
    public List<Long> map(List<CustomerSegmentView> customerSegmentViewList) {

        return customerSegmentViewList
                .stream()
                .map(CustomerSegmentView::getSegmentId)
                .toList();
    }
}
