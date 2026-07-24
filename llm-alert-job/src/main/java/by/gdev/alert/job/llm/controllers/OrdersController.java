package by.gdev.alert.job.llm.controllers;

import by.gdev.alert.job.llm.domain.dto.order.AiOrderRequest;
import by.gdev.alert.job.llm.service.aiautoreply.AutoReplyPipeline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai/api/orders")
@Slf4j
@Tag(name = "Orders", description = "Управление заказами для AI-обработки")
public class OrdersController {

    @Autowired
    AutoReplyPipeline autoReplyPipeline;


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