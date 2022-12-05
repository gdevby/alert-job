package by.gdev.gateway.conf;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
//@EnableWebSecurity
@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ServerLogoutSuccessHandler handler) {
		http
				.authorizeExchange()
				.pathMatchers("/alert-job-test-service/public/**")
				.permitAll()
//				.pathMatchers("test-service/secure/**")
//				.hasAnyRole("admin-test-service-role")
			.and()
				.authorizeExchange()
				.anyExchange()
				.authenticated()
			.and()
				.oauth2Login() // to redirect to oauth2 login page.
			.and()
				.logout()
				.logoutSuccessHandler(handler)
				.and()
				.oauth2ResourceServer()
					.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()));;
		return http.build();
	}
	private Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
		ReactiveJwtAuthenticationConverter jwtConverter =  new ReactiveJwtAuthenticationConverter();
		jwtConverter.setJwtGrantedAuthoritiesConverter(new Converter<Jwt, Flux<GrantedAuthority>>() {
			
			@Override
			public Flux<GrantedAuthority> convert(Jwt source) {
				List<String> list=  ((Map<String, List<String>>) source.getClaims().get("realm_access")).get("roles");
				return Flux.fromStream(list.stream()).map(vv->{
					return "ROLES_" + vv; 
				}).map(SimpleGrantedAuthority::new);
			}
		});
		
		return jwtConverter;
	}
	@Bean
	public ServerLogoutSuccessHandler keycloakLogoutSuccessHandler(ReactiveClientRegistrationRepository repository) {

        OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedServerLogoutSuccessHandler(repository);

        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/logout.html");

        return oidcLogoutSuccessHandler;
    }
}