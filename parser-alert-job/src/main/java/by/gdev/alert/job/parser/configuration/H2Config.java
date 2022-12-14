package by.gdev.alert.job.parser.configuration;

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
	        this.server.stop();
	    }
	
}
