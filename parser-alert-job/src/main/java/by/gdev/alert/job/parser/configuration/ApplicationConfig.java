package by.gdev.alert.job.parser.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FLRU;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCEHUNT;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCER;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_FREELANCERU;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_HUBR;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_KWORK;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_WEBLANCER;
import static by.gdev.alert.job.parser.util.ParserStringUtils.COUNTER_YOUDO;
import static by.gdev.alert.job.parser.util.ParserStringUtils.PROXY_CLIENT;

@Configuration
@EnableScheduling
@Slf4j

public class ApplicationConfig {

	@Value("${proxy.host}")
	private String proxyHost;
	@Value("${proxy.port}")
	private int proxyPort;
	@Value("${proxy.username}")
	private String proxyUsername;
	@Value("${proxy.password}")
	private String proxyPassword;

	@Autowired
	private ApplicationContext context;
	@Autowired
	private MeterRegistry meterRegistry;

	@Bean
	public WebClient createWebClient() {
		reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
				.followRedirect(true)
				.doOnConnected(con -> con.addHandlerFirst(new ReadTimeoutHandler(60, TimeUnit.SECONDS)))
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000).option(ChannelOption.SO_KEEPALIVE, true)
				.option(EpollChannelOption.TCP_KEEPIDLE, 300).option(EpollChannelOption.TCP_KEEPINTVL, 60)
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
	public ModelMapper createModelMapper() {
		return new ModelMapper();
	}

	@Bean
	RestTemplate restTemplate() {
		if (StringUtils.isBlank(proxyHost)) {
			log.warn("proxy host is empty, it means that FreelancehuntOrderParcer should work without proxy.");
			return new RestTemplate();
		}
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort),
				new UsernamePasswordCredentials(proxyUsername, proxyPassword));
		HttpHost myProxy = new HttpHost(proxyHost, proxyPort);
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		clientBuilder.setProxy(myProxy).setDefaultCredentialsProvider(credsProvider).disableCookieManagement();
		HttpClient httpClient = clientBuilder.build();
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setHttpClient(httpClient);
		return new RestTemplate(factory);
	}

	@PostConstruct
	void init() {
		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
		beanFactory.registerSingleton(COUNTER_HUBR, meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "hubr"));
		beanFactory.registerSingleton(COUNTER_FLRU, meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "flru"));
		beanFactory.registerSingleton(COUNTER_FREELANCERU,
				meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "freelanceru"));
		beanFactory.registerSingleton(COUNTER_WEBLANCER,
				meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "weblancer"));
		beanFactory.registerSingleton(COUNTER_FREELANCEHUNT,
				meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "freelancehun"));
		beanFactory.registerSingleton(COUNTER_YOUDO, meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "youdo"));
		beanFactory.registerSingleton(COUNTER_KWORK, meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "kwork"));
		beanFactory.registerSingleton(COUNTER_FREELANCER,
				meterRegistry.counter(PROXY_CLIENT, PROXY_CLIENT, "freelancer"));
	}
}
