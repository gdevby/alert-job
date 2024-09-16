package by.gdev.alert.job.parser.util.proxy;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class FileReader {

    @Autowired
    private ResourceLoader resourceLoader;

    public List<String> read(String resource){
        try (InputStream inputStream = resourceLoader.getResource(resource).getInputStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            Stream<String> lines = bufferedReader.lines();
            List<String> listOfLines = lines.toList();
            return listOfLines;
        } catch (IOException e) {
            log.error("Cannot read file: {}", resource);
            throw new RuntimeException(e);
        }
    }
}
