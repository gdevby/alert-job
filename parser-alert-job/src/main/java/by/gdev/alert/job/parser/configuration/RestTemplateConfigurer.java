package by.gdev.alert.job.parser.configuration;

import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.alert.job.parser.util.proxy.ProxySupplier;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

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

        CredentialsProvider credsProvider = getCredentialsProvider(proxyCredentials);
        HttpHost myProxy = new HttpHost(proxyCredentials.getHost(), proxyCredentials.getPort());

        HttpClient httpClient = createHttpClient(myProxy, credsProvider);

        HttpComponentsClientHttpRequestFactory requestFactory = getRequestFactory();
        requestFactory.setHttpClient(httpClient);


        return new RestTemplate(requestFactory);
    }

    private CredentialsProvider getCredentialsProvider(ProxyCredentials proxyCredentials){
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(proxyCredentials.getHost(), proxyCredentials.getPort()),
                                    new UsernamePasswordCredentials(proxyCredentials.getUsername(), proxyCredentials.getPassword()));

        return credsProvider;
    }

    private CloseableHttpClient createHttpClient(HttpHost myProxy, CredentialsProvider credsProvider) {
        return HttpClientBuilder
                .create()
                .setProxy(myProxy)
                .setDefaultCredentialsProvider(credsProvider)
                .disableCookieManagement()
                .build();
    }

    private HttpComponentsClientHttpRequestFactory getRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientRequestFactory = new HttpComponentsClientHttpRequestFactory();

        clientRequestFactory.setConnectTimeout(connectTimeout);
        clientRequestFactory.setReadTimeout(socketTimeout);

        return clientRequestFactory;
    }

    public RestTemplate getRestTemplate(){
        return new RestTemplate(getRequestFactory());
    }
    
}
