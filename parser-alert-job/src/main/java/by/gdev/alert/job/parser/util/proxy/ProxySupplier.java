package by.gdev.alert.job.parser.util.proxy;

import by.gdev.alert.job.parser.proxy.db.ProxyState;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ProxySupplier {

    @Value("${parser.proxy.mode:file}")
    private String mode;

    @Value("${parser.proxy.file.path}")
    private String proxyFile;

    @Value("${parser.proxy.url}")
    private String proxyUrl;

    @Autowired
    private FileReader fileReader;

    private List<ProxyCredentials> proxies = new ArrayList<>();
    private final ProxyParser proxyParser = new ProxyParser();

    private int index = 0;

    private synchronized List<String> downloadProxyLines() throws IOException {
        URL url = new URL(proxyUrl);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return reader.lines().toList();
        }
    }

    private void parse(List<String> proxiesLines) {
        for (String proxiesLine : proxiesLines) {
            ProxyCredentials proxyCredentials = proxyParser.parse(proxiesLine);
            if (proxyCredentials != null){
                proxies.add(proxyCredentials);
            }
        }
    }

    public synchronized ProxyCredentials get() {
        if (proxies.isEmpty()) {
            List<String> proxiesLines = fileReader.read(proxyFile);
            parse(proxiesLines);
        }

        ProxyCredentials proxyCredentials = getNextProxyCredentials();
        return proxyCredentials;
    }

    @PostConstruct
    public void init() {
        if ("url".equalsIgnoreCase(mode)) {
            downloadProxies();
            log.debug("Прокси загружены по URL: {}", proxyUrl);
        }
        else {
            readProxies();
            log.debug("Прокси загружены из файла: {}", proxyFile);
        }
    }

    public synchronized List<ProxyCredentials> downloadProxies() {
        try {
            if (proxies.isEmpty()) {
                List<String> proxiesLines = downloadProxyLines();
                parse(proxiesLines);
            }
        }
        catch (IOException ioException){
            log.error("Ошибка загрузки прокси по URL {}: {}", proxyUrl, ioException.getMessage());
            return List.of();
        }
        return proxies;
    }

    public synchronized List<ProxyCredentials> readProxies() {
        if (proxies.isEmpty()) {
            List<String> proxiesLines = fileReader.read(proxyFile);
            parse(proxiesLines);
        }
        return proxies;
    }

    public synchronized List<ProxyCredentials> getProxies() {
        return proxies;
    }

    public synchronized List<ProxyCredentials> getWorkingProxies() {
        return getProxies().stream()
                .filter(p -> p.getState() == ProxyState.ACTIVE
                        || p.getState() == ProxyState.NEW
                        || p.getState() == ProxyState.WARMING_UP)
                .toList();
    }

    private ProxyCredentials getNextProxyCredentials() {
        if (index >= proxies.size() - 1) {
            index = 0;
        }
        ProxyCredentials proxyCredentials = proxies.get(index);
        index++;
        return proxyCredentials;
    }

}

