package by.gdev.alert.job.core.configuration;

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
	        this.server = Server.createWebServer("-webPort", "8083", "-tcpAllowOthers").start();
	    }
	 
	    @EventListener(ContextClosedEvent.class)
	    public void stop() {
	        this.server.stop();
	    }

}
