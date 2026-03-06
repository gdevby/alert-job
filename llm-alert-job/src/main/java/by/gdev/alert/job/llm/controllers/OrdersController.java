package by.gdev.alert.job.llm.controllers;

import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import by.gdev.alert.job.llm.service.aiautoreply.AutoReplyPipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/api/orders")
@Slf4j
public class OrdersController {

    @Autowired
    AutoReplyPipeline autoReplyPipeline;

    @PostMapping(produces = "application/json")
    public ResponseEntity<Map<String, String>> receiveOrders(
            @RequestBody List<OrderDTO> orders) {
        log.info("Принято для AI {} заказов", orders.size());
        orders.forEach(autoReplyPipeline::process);
        return ResponseEntity.ok(Map.of(
                "message", "Заказы приняты",
                "count", String.valueOf(orders.size())
        ));
    }
}