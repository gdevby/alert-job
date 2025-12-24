package by.gdev.alert.job.parser.util.proxy;

import by.gdev.alert.job.parser.proxy.db.ProxyState;

public class ProxyParser {
    public ProxyCredentials parse(String proxyLine){

        try {
            String[] split = proxyLine.split(":");
            String host = split[0];
            int port = Integer.parseInt(split[1]);
            String username = split[2];
            String password = split[3];

            return ProxyCredentials.builder()
                    .username(username)
                    .password(password)
                    .host(host)
                    .port(port)
                    .state(ProxyState.NEW)
                    .build();
        }
        catch (Exception ex){
            return null;
        }
    }
}
