package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.order.search.dto.OrderSearchRequest;
import by.gdev.alert.job.parser.service.order.search.OrderSearchService;
import by.gdev.alert.job.parser.service.order.search.dto.PageResponse;
import by.gdev.common.model.OrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderSearchController {

    private final OrderSearchService orderSearchService;

    @PostMapping("/search")
    public PageResponse<OrderDTO> search(@RequestBody OrderSearchRequest request) {

        if (!"TITLE".equalsIgnoreCase(request.getMode())
                && !"DESCRIPTION".equalsIgnoreCase(request.getMode())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Некорректный режим поиска: " + request.getMode()
            );
        }
        return orderSearchService.search(request);
    }
}
