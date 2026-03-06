package by.gdev.alert.job.llm.controllers;

import by.gdev.alert.job.llm.service.LlmService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LlmController {

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    @GetMapping("/llm")
    public String ask(@RequestParam String q) {
        return llmService.ask(q);
    }
}
