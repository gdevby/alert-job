package by.gdev.alert.job.parser.configuration;

import java.util.Objects;

import org.h2.tools.Server;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class H2Config {

	private Server server;

	@EventListener(org.springframework.context.event.ContextRefreshedEvent.class)
	public void start() throws java.sql.SQLException {
		this.server = Server.createWebServer("-webPort", "8082", "-tcpAllowOthers").start();
	}

	@EventListener(org.springframework.context.event.ContextClosedEvent.class)
	public void stop() {
		if (Objects.nonNull(server))
			server.stop();
	}

}
