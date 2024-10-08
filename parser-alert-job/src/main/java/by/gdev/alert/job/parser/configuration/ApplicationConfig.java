package by.gdev.alert.job.parser.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

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

	@Value("${request.timeout.socket}")
	private int socketTimeout;
	@Value("${request.timeout.connect}")
	private int connectTimeout;

	@Autowired
	private ApplicationContext context;
	@Autowired
	private MeterRegistry meterRegistry;

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
		HttpHost myProxy = new HttpHost(proxyHost, proxyPort);
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort),
				new UsernamePasswordCredentials(proxyUsername, proxyPassword.toCharArray()));

		HttpClient httpClient = getHttpClient(myProxy, credsProvider);

		return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
	}


	private HttpClient getHttpClient(HttpHost myProxy, CredentialsProvider credsProvider) {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

		return HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(RequestConfig.custom()
						.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
						.setResponseTimeout(socketTimeout, TimeUnit.MILLISECONDS)
						.build())
				.setProxy(myProxy)
				.setDefaultCredentialsProvider(credsProvider)
				.build();
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
