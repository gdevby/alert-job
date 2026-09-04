package by.gdev.alert.job.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("plainRestTemplate")
    public RestTemplate plainRestTemplate(
            @Value("${credential.validation.timeout.ms:180000}") int credentialValidationTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(credentialValidationTimeoutMs);
        factory.setReadTimeout(credentialValidationTimeoutMs);
        return new RestTemplate(factory);
    }

}
