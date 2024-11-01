package by.gdev.alert.job.parser.configuration;

import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.alert.job.parser.util.proxy.ProxySupplier;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Component
public class RestTemplateFactory {

    @Value("${request.timeout.socket}")
    private int socketTimeout;
    @Value("${request.timeout.connect}")
    private int connectTimeout;

    @Autowired
    private ProxySupplier proxySupplier;

    private RestTemplate restTemplate;

    public RestTemplate getRestTemplate(boolean proxy) {
        return proxy ? getRestTemplateWithProxy() : getRestTemplate();
    }

    private RestTemplate getRestTemplateWithProxy() {
        ProxyCredentials proxyCredentials = proxySupplier.get();

        HttpHost myProxy = new HttpHost(proxyCredentials.getHost(), proxyCredentials.getPort());
        CredentialsProvider credsProvider = getCredentialsProvider(proxyCredentials);

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(getHttpClient(myProxy, credsProvider)));
    }

    private RestTemplate getRestTemplate() {
        return restTemplate == null ? restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(getHttpClient())) : restTemplate;
    }

    private BasicCredentialsProvider getCredentialsProvider(ProxyCredentials proxyCredentials) {
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(proxyCredentials.getHost(), proxyCredentials.getPort()),
                new UsernamePasswordCredentials(proxyCredentials.getUsername(), proxyCredentials.getPassword().toCharArray()));

        return credsProvider;
    }

    private HttpClient getHttpClient(HttpHost myProxy, CredentialsProvider credsProvider) {
        return getHttpClientBuilder()
                .setProxy(myProxy)
                .setDefaultCredentialsProvider(credsProvider)
                .build();
    }

    private HttpClient getHttpClient() {
        return getHttpClientBuilder()
                .build();
    }

    private HttpClientBuilder getHttpClientBuilder() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .setResponseTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                        .build()
                );
    }


}
