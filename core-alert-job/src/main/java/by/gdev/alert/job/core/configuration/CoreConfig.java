package by.gdev.alert.job.core.configuration;

import java.util.concurrent.TimeUnit;

import org.hibernate.collection.spi.PersistentCollection;
import org.modelmapper.Condition;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableScheduling
public class CoreConfig {

	@Bean
	@LoadBalanced
	WebClient createWebClient() {
		HttpClient httpClient = HttpClient.create().followRedirect(true)
				.doOnConnected(con -> con.addHandlerFirst(new ReadTimeoutHandler(500, TimeUnit.SECONDS)))
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000).option(ChannelOption.SO_KEEPALIVE, true)
				.option(EpollChannelOption.TCP_KEEPIDLE, 500).option(EpollChannelOption.TCP_KEEPINTVL, 60)
				.option(EpollChannelOption.TCP_KEEPCNT, 8);

		ReactorClientHttpConnector conn = new ReactorClientHttpConnector(httpClient);
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(); // Here comes your base url
		factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		return WebClient.builder().uriBuilderFactory(factory)
				.exchangeStrategies(ExchangeStrategies.builder().codecs(codecs -> {
					codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024 * 1024);
					codecs.defaultCodecs().jaxb2Encoder(new Jaxb2XmlEncoder());
					codecs.defaultCodecs().jaxb2Decoder(new Jaxb2XmlDecoder());
				}).build()).clientConnector(conn).build();
	}

	@Bean
	ModelMapper createModelMapper() {
		ModelMapper mapper = new ModelMapper();
		mapper.getConfiguration().setSkipNullEnabled(true).setPropertyCondition(new Condition<Object, Object>() {
			@Override
			public boolean applies(MappingContext<Object, Object> context) {
				return (!(context.getSource() instanceof PersistentCollection)
						|| ((PersistentCollection) context.getSource()).wasInitialized());
			}
		});
		return mapper;
	}

	@Bean
	ApplicationProperty getApplicationProperty() {
		return new ApplicationProperty();
	}

	@Bean
	ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(2);
		return taskScheduler;
	}
}
