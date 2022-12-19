package by.gdev.gateway.conf;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

@Configuration
//@EnableWebSecurity
@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {
	@Value("${gateway.logout}")
	private String logoutURI;

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
			ServerLogoutSuccessHandler handler) {
		http.csrf().disable().authorizeExchange()
				.pathMatchers("/alert-job-test-service/public/**", "/logout.html", "/", "/favicon.ico").permitAll()
				.pathMatchers("test-service/secure/**").hasAnyRole("admin-test-service-role").and().authorizeExchange()
				.pathMatchers("/actuator/**").permitAll().anyExchange().authenticated().and().oauth2Login().and() // to
																													// redirect
																													// to
																													// oauth2
																													// login
																													// page.
				.logout().logoutSuccessHandler(handler).and();
		return http.build();
	}

	@SuppressWarnings("deprecation")
	@Bean
	public ServerLogoutSuccessHandler keycloakLogoutSuccessHandler(ReactiveClientRegistrationRepository repository) {

		OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedServerLogoutSuccessHandler(
				repository);

		try {
			oidcLogoutSuccessHandler.setPostLogoutRedirectUri(new URI(logoutURI));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return oidcLogoutSuccessHandler;
	}
}