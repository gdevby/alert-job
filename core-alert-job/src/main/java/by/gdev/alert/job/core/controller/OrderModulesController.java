package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.service.OrderModulesService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@Slf4j
@Hidden
public class OrderModulesController {

    private final OrderModulesService orderModulesService;

    @GetMapping("/auto-reply/users")
    public List<String> getUsersWithAutoReplyEnabled() {
        return orderModulesService.findDistinctUserUuidsWithAutoReplyEnabled();
    }
}