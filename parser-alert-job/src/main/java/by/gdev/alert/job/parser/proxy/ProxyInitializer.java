package by.gdev.alert.job.parser.proxy;

import by.gdev.alert.job.parser.proxy.db.ProxyInfo;
import by.gdev.alert.job.parser.proxy.db.ProxyState;
import by.gdev.alert.job.parser.proxy.db.ProxyType;
import by.gdev.alert.job.parser.proxy.repository.ProxyInfoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class ProxyInitializer {
    private final ProxyInfoRepository proxyInfoRepository;

    @Value("${proxy.file.path}")
    private String proxyFilePath;

    @PostConstruct
    public void init() throws IOException {
        for (String line : Files.readAllLines(Path.of(proxyFilePath))) {
            if (line.isBlank()) continue;

            // формат строки: ip:port[:username:password]
            String[] parts = line.split(":");
            ProxyInfo proxy = new ProxyInfo();
            proxy.setIp(parts[0]);
            proxy.setPort(Integer.parseInt(parts[1]));

            if (parts.length > 2) proxy.setUsername(parts[2]);
            if (parts.length > 3) proxy.setPassword(parts[3]);

            proxy.setType(ProxyType.HTTP);
            proxy.setState(ProxyState.NEW);

            proxyInfoRepository.save(proxy);
            proxyInfoRepository.findAll();
        }
    }
}
