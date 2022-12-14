package by.gdev.alert.job.parser.configuration;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class ApplicationConfig {
	
	
	@Bean
	public WebClient createWebClient() {
		HttpClient httpClient = HttpClient.create().followRedirect(true)
				//TODO why?
				.doOnConnected(con -> con.addHandler(new ReadTimeoutHandler(60, TimeUnit.SECONDS)))
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000).option(ChannelOption.SO_KEEPALIVE, true)
				.option(EpollChannelOption.TCP_KEEPIDLE, 300).option(EpollChannelOption.TCP_KEEPINTVL, 60)
				.option(EpollChannelOption.TCP_KEEPCNT, 8);
		
		ReactorClientHttpConnector conn = new ReactorClientHttpConnector(httpClient);
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(); // Here comes your base url
		factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		return WebClient.builder().uriBuilderFactory(factory)
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(codecs -> {
							codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024 * 1024);
							codecs.defaultCodecs().jaxb2Encoder(new Jaxb2XmlEncoder());
							codecs.defaultCodecs().jaxb2Decoder(new Jaxb2XmlDecoder());
						}).build())
				.clientConnector(conn).build();
	}
}
