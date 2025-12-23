package by.gdev.alert.job.parser.proxy.db;

public enum ProxyState {
    NEW,            // только что добавлен, ещё не проверен
    WARMING_UP,     // проходит прогрев (тестовые запросы)
    ACTIVE,         // рабочий, успешно используется
    INACTIVE,       // временно отключён (например, ручное выключение)
    BANNED,         // получил бан на сайте
    QUARANTINE,     // в карантине после N ошибок
    FAILED          // окончательно нерабочий, исключён из пула
}

