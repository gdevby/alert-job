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

@Configuration
//@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
			ServerLogoutSuccessHandler handler) {
		http.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(req -> req.pathMatchers("/logout.html", "/", "/favicon.ico", "/actuator/**",
						"/core-alert-job/api/user/test", "/parser/api/orders/statistics").permitAll())

				.authorizeExchange(req -> req.anyExchange().authenticated()).oauth2Login(Customizer.withDefaults())
				.logout(logout -> logout.logoutSuccessHandler(handler));
		return http.build();
	}

	@Bean
	public ServerLogoutSuccessHandler keycloakLogoutSuccessHandler(ReactiveClientRegistrationRepository repository) {

		OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedServerLogoutSuccessHandler(
				repository);

		oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");

		return oidcLogoutSuccessHandler;
	}

}