package by.gdev.common.service.proxy.supplier;

import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.model.proxy.ProxyState;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public synchronized void replaceProxies(List<ProxyCredentials> newList) {
        if (newList == null || newList.isEmpty()) {
            log.error("Попытка заменить список прокси на ПУСТОЙ! Обновление отменено.");
            return;
        }

        List<ProxyCredentials> oldList = new ArrayList<>(this.proxies);
        logProxyDiff(oldList, newList);
        this.proxies = new ArrayList<>(newList);
        this.index = 0;
        log.debug("Список прокси заменен. Новый размер: {}", proxies.size());
    }


    public synchronized List<ProxyCredentials> loadFreshProxies() {
        boolean useUrlForRefresh = true;
        List<String> lines;
        try {
            if (useUrlForRefresh && proxyUrl != null && !proxyUrl.isBlank()) {
                // mode=file - обновляем из URL
                // mode=url  - обновляем из URL
                log.debug("Обновление прокси: загрузка из URL {}", proxyUrl);
                lines = downloadProxyLines();
                log.debug("Загружено {} строк прокси из URL {}", lines.size(), proxyUrl);
            } else {
                // fallback если URL не указан
                log.debug("Обновление прокси: URL не задан, читаем файл {}", proxyFile);
                lines = fileReader.read(proxyFile);
                log.debug("Прочитано {} строк прокси из файла {}", lines.size(), proxyFile);
            }
        } catch (Exception e) {
            log.error("Ошибка загрузки прокси при обновлении: {}", e.getMessage());
            return List.of();
        }

        List<ProxyCredentials> list = new ArrayList<>();
        for (String line : lines) {
            ProxyCredentials pc = proxyParser.parse(line);
            if (pc != null) list.add(pc);
        }

        log.debug("Распарсено {} прокси при обновлении", list.size());
        if (list.isEmpty()) {
            log.warn("ПРЕДУПРЕЖДЕНИЕ: Загруженный список прокси пустой!");
        }
        return list;
    }


    public void logProxyDiff(List<ProxyCredentials> oldList, List<ProxyCredentials> newList) {
        if (oldList == null || oldList.isEmpty()) {
            log.info("Old proxy list was empty. New list size: {}", newList.size());
            return;
        }

        // Преобразуем в строки для сравнения
        Set<String> oldSet = oldList.stream().map(ProxyCredentials::toString).collect(Collectors.toSet());
        Set<String> newSet = newList.stream().map(ProxyCredentials::toString).collect(Collectors.toSet());

        // Найти добавленные
        Set<String> added = new HashSet<>(newSet);
        added.removeAll(oldSet);

        // Найти удалённые
        Set<String> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);

        log.info("Proxy diff: old={}, new={}, added={}, removed={}",
                oldList.size(), newList.size(), added.size(), removed.size());

        if (!added.isEmpty()) {
            log.info("Added proxies:\n{}", String.join("\n", added));
        }

        if (!removed.isEmpty()) {
            log.info("Removed proxies:\n{}", String.join("\n", removed));
        }
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

