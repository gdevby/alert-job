package by.gdev.alert.job.parser.util.proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class ProxySupplier {

    @Value("${proxy.file}")
    private String proxyFile;
    @Autowired
    private FileReader fileReader;

    private List<ProxyCredentials> proxies = new ArrayList<>();
    private final ProxyParser proxyParser = new ProxyParser();

    private void parse(List<String> proxiesLines) {
        for (String proxiesLine : proxiesLines) {
            ProxyCredentials proxyCredentials = proxyParser.parse(proxiesLine);
            proxies.add(proxyCredentials);
        }
    }

    public ProxyCredentials get() {
        if (proxies.isEmpty()) {
            List<String> proxiesLines = fileReader.read(proxyFile);
            parse(proxiesLines);
        }

        ProxyCredentials proxyCredentials = getRandomProxyCredentials();
        return proxyCredentials;
    }

    private ProxyCredentials getRandomProxyCredentials() {
        Random random = new Random();
        int randomIndex = random.nextInt(proxies.size());
        ProxyCredentials proxyCredentials = proxies.get(randomIndex);
        return proxyCredentials;
    }

}

