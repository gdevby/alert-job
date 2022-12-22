package by.gdev.alert.job.notification.config;

import java.util.concurrent.TimeUnit;

import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class BeanConfiguration {

	@Bean
	public ApplicationProperty property() {
		return new ApplicationProperty();
	}

	@Bean
	public Mailer test() {
		return MailerBuilder.withSMTPServerHost("smtp.gmail.com").withSMTPServerPort(587)
				.withSMTPServerUsername(property().getMailLogin()).withSMTPServerPassword(property().getMailPassword())
				.withTransportStrategy(TransportStrategy.SMTP_TLS).withDebugLogging(false).buildMailer();

	}
	//TODO test in future to remove from warning
	@SuppressWarnings("deprecation")
	@Bean
	public WebClient createWebClient() {
		HttpClient httpClient = HttpClient.create().followRedirect(true)
				.doOnConnected(con -> con.addHandler(new ReadTimeoutHandler(60, TimeUnit.SECONDS)))
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000).option(ChannelOption.SO_KEEPALIVE, true)
				.option(EpollChannelOption.TCP_KEEPIDLE, 300).option(EpollChannelOption.TCP_KEEPINTVL, 60)
				.option(EpollChannelOption.TCP_KEEPCNT, 8);

		ReactorClientHttpConnector conn = new ReactorClientHttpConnector(httpClient);
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(); // Here comes your base url
		factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		return WebClient.builder().uriBuilderFactory(factory)
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024 * 1024)).build())
				.clientConnector(conn).build();
	}

}
