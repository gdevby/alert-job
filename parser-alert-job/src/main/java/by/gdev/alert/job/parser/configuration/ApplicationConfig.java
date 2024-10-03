package by.gdev.alert.job.parser.configuration;

import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
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
import org.springframework.beans.factory.annotation.Value;
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

import java.util.concurrent.TimeUnit;

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
		HttpHost myProxy = new HttpHost(proxyHost, proxyPort);
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort),
				new UsernamePasswordCredentials(proxyUsername, proxyPassword.toCharArray()));

		HttpClient httpClient = getHttpClient(myProxy, credsProvider);

		return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
	}

	private BasicCredentialsProvider getCredentialsProvider(ProxyCredentials proxyCredentials){
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(proxyCredentials.getHost(), proxyCredentials.getPort()),
				new UsernamePasswordCredentials(proxyCredentials.getUsername(), proxyCredentials.getPassword().toCharArray()));

		return credsProvider;
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

}
