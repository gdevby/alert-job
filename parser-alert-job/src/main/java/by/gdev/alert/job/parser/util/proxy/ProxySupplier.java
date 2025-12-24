package by.gdev.alert.job.parser.util.proxy;

import by.gdev.alert.job.parser.proxy.db.ProxyState;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProxySupplier {

    @Value("${parser.proxy.file.path}")
    private String proxyFile;
    @Autowired
    private FileReader fileReader;

    private List<ProxyCredentials> proxies = new ArrayList<>();
    private final ProxyParser proxyParser = new ProxyParser();

    private int index = 0;

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
        readProxies();
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

