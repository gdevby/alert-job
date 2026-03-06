package by.gdev.alert.job.llm.service.data;

import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataService {

    public List<OrderDTO> getOrders(){
        return List.of();
    }
}
