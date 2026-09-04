package by.gdev.common.service.proxy.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class ProxyListUpdatedEvent extends ApplicationEvent {
    private final List<?> updatedProxies;

    public ProxyListUpdatedEvent(Object source, List<?> updatedProxies) {
        super(source);
        this.updatedProxies = updatedProxies;
    }
}