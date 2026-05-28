package by.gdev.alert.job.llm.domain.dto.order;

import lombok.Data;

import java.util.List;

@Data
public class AiOrderRequest {
    private AiAppUserDTO user;
    private AiOrderModulesDTO module;
    private Long credentialId;
    private Long templateId;
    private List<OrderDTO> orders;
}
