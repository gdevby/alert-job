package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.service.OrderProcessor;
import by.gdev.common.model.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrdersController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private OrderProcessor orderProcessor;

    @PostMapping(produces = "application/json")
    public ResponseEntity<Map<String, String>> createOrders(
            @RequestParam("site") String site,
            @RequestBody List<OrderDTO> orders) {

        log.info("Принято {} заказов от {}", orders.size(), site);
        Set<AppUser> users = userRepository.findAllUsersEagerOrderModules();
        orderProcessor.forEachOrders(users, orders);
        return ResponseEntity.ok(Map.of(
                "message", "Заказы приняты",
                "site", site,
                "count", String.valueOf(orders.size())
        ));
    }
}

