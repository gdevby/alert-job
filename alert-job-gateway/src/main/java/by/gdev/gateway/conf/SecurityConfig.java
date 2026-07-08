package by.gdev.gateway.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            ServerLogoutSuccessHandler handler) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(req ->
                        req.pathMatchers(getPermittedPaths("llm", "core"))
                                .permitAll()
                                .pathMatchers("/core-alert-job/api/orders",
                                        "/core-alert-job/api/cleanup")
                                .denyAll()
                                .anyExchange()
                                .authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessHandler(handler));
        return http.build();
    }

    /**
     * Возвращает массив путей, которые должны быть доступны без аутентификации.
     * Включает базовые общедоступные пути и Swagger-пути для указанных модулей.
     *
     * @param swaggerModules имена модулей, для которых нужно разрешить Swagger-документацию
     * @return массив строк с путями для permitAll
     */
    private String[] getPermittedPaths(String... swaggerModules) {
        List<String> paths = new ArrayList<>();

        // Базовые общедоступные пути
        paths.addAll(Arrays.asList(
                "/logout.html",
                "/",
                "/favicon.png",
                "/actuator/**",
                "/core-alert-job/api/user/test",
                "/parser/api/orders/statistics"
        ));

        // Добавляем Swagger-пути для каждого переданного модуля
        for (String module : swaggerModules) {
            paths.addAll(Arrays.asList(swaggerPaths(module)));
        }
        return paths.toArray(new String[0]);
    }

    private String[] swaggerPaths(String moduleName) {
        return new String[]{
                "/" + moduleName + "/swagger-ui.html",
                "/" + moduleName + "/swagger-ui/**",
                "/" + moduleName + "/webjars/**",
                "/" + moduleName + "/v3/api-docs/**"
        };
    }

    @Bean
    public ServerLogoutSuccessHandler keycloakLogoutSuccessHandler(ReactiveClientRegistrationRepository repository) {

        OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedServerLogoutSuccessHandler(repository);

        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");

        return oidcLogoutSuccessHandler;
    }

}