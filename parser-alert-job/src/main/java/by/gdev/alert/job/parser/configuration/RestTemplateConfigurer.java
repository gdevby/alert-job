package by.gdev.alert.job.parser.configuration;

import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.alert.job.parser.util.proxy.ProxySupplier;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
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
public class RestTemplateConfigurer {

    @Value("${request.timeout.socket}")
    private int socketTimeout;
    @Value("${request.timeout.connect}")
    private int connectTimeout;

    @Autowired
    private ProxySupplier proxySupplier;

    public RestTemplate getRestTemplateWithProxy(){
        ProxyCredentials proxyCredentials = proxySupplier.get();

        HttpHost myProxy = new HttpHost(proxyCredentials.getHost(), proxyCredentials.getPort());
        CredentialsProvider credsProvider = getCredentialsProvider(proxyCredentials);

        HttpClient httpClient = getHttpClient(myProxy, credsProvider);

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

    }

    public RestTemplate getRestTemplate(){
        HttpClient httpClient = getHttpClient();
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

    private HttpClient getHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .setResponseTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                        .build())
                .build();
    }



}
