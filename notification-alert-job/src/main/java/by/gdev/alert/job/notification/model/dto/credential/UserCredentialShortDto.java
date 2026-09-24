package by.gdev.alert.job.notification.model.dto.credential;

import lombok.Data;

@Data
public class UserCredentialShortDto {
    private Long id;
    private String login;
    private SiteRef site;

    @Data
    public static class SiteRef {
        private Long id;
        private String name;
    }
}
