package by.gdev.alert.job.parser.scheduller.parser;

import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDispatcher {

    private final CoreApiService coreApiService;

    public void dispatch(List<OrderDTO> orders, SiteName siteName) {
        if (orders == null || orders.isEmpty()) {
            log.debug("Нет новых заказов от {}", siteName);
            return;
        }

        try {
            //coreApiService.sendOrders(orders, siteName);
            coreApiService.sendOrdersInBatches(orders, siteName);
            log.debug("Отправлено {} заказов от {}", orders.size(), siteName);
        } catch (Exception e) {
            log.error("Ошибка при отправке заказов от {}: {}", siteName, e.getMessage(), e);
        }
    }
}

