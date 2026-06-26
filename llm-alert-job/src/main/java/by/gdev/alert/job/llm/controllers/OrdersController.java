package by.gdev.alert.job.llm.controllers;

import by.gdev.alert.job.llm.domain.dto.order.AiOrderRequest;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import by.gdev.alert.job.llm.service.aiautoreply.AutoReplyPipeline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(
            summary = "Получить список заказов (устаревший метод - не используется или используется для тестов)",
            description = """
                    Принимает список заказов (OrderDTO) и отправляет каждый заказ в AutoReplyPipeline.
                    Используется для массовой обработки заказов без контекста пользователя.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Заказы успешно приняты",
            content = @Content(schema = @Schema(implementation = Map.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка обработки входных данных"
    )
    @Deprecated
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

    @Operation(
            summary = "Получить заказы с дополнительной информацией (пользователь, модуль, шаблон и т.д.)",
            description = """
                    Принимает расширенный объект AiOrderRequest, содержащий:
                    • список заказов,
                    • данные пользователя,
                    • данные модуля.
                    
                    Используется для более точной обработки заказов с учётом контекста.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "AiOrderRequest успешно принят",
            content = @Content(schema = @Schema(implementation = Map.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка обработки запроса"
    )
    @PostMapping(value = "/context", produces = "application/json")
    public ResponseEntity<Map<String, String>> receiveAiOrderRequest(
            @RequestBody AiOrderRequest request) {

        log.debug(
                "AI получил {} заказов от пользователя {} (модуль: {})",
                request.getOrders().size(),
                request.getUser().getEmail(),
                request.getModule().getName()
        );

        autoReplyPipeline.process(request);

        return ResponseEntity.ok(Map.of(
                "message", "AiOrderRequest принят",
                "count", String.valueOf(request.getOrders().size())
        ));
    }

}