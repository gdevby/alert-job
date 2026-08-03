package by.gdev.alert.job.llm.config;

import java.util.Objects;

import org.h2.tools.Server;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class H2Config {

	private Server server;

	@EventListener(ContextRefreshedEvent.class)
	public void start() throws java.sql.SQLException {
		this.server = Server.createWebServer("-webPort", "8084", "-tcpAllowOthers").start();
	}

	@EventListener(ContextClosedEvent.class)
	public void stop() {
		if (Objects.nonNull(server)) {
			server.stop();
		}
	}
}
