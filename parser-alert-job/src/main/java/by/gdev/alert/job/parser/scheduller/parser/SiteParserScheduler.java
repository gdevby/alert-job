package by.gdev.alert.job.parser.scheduller.parser;

import by.gdev.alert.job.parser.scheduller.parser.properties.ParserScheduleProperties;
import by.gdev.alert.job.parser.service.order.SiteParser;
import by.gdev.common.model.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;

import java.util.Date;
import java.util.List;

@Slf4j
public class SiteParserScheduler {

    private final SiteParser parser;
    private final OrderDispatcher dispatcher;


    public SiteParserScheduler(SiteParser parser,
                               OrderDispatcher dispatcher,
                               ParserScheduleProperties props,
                               TaskScheduler taskScheduler) {
        this.parser = parser;
        this.dispatcher = dispatcher;

        // регистрируем задачу прямо в конструкторе
        if (parser.isActive()){
            taskScheduler.scheduleWithFixedDelay(
                    this::runParser,
                    new Date(System.currentTimeMillis() + props.getInitialDelayMillis()),
                    props.getFixedDelayMillis());

            log.debug("Шедулер для {} запущен: initialDelay={}s, fixedDelay={}s",
                    parser.getSiteName(), props.getInitialDelaySeconds(), props.getFixedDelaySeconds());
        }
        else log.debug("Парсер для {} ВЫКЛЮЧЕН..", parser.getSiteName());

    }

    private void runParser() {
        log.debug("Запуск парсера {} в потоке {}", parser.getSiteName(), Thread.currentThread().getName());
        try {
            List<OrderDTO> orders = parser.parse();
            dispatcher.dispatch(orders, parser.getSiteName());
            log.debug("Парсер {} завершил работу, найдено {} заказов", parser.getSiteName(), orders.size());
        } catch (Exception e) {
            log.error("Ошибка парсера {}: {}", parser.getSiteName(), e.getMessage(), e);
        }
    }
}