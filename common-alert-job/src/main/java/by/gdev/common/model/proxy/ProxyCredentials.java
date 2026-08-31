package by.gdev.common.model.proxy;

import by.gdev.common.service.proxy.ProxySource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ProxyCredentials {
    private String username;
    private String password;
    private String host;
    private int port;
    private ProxyState state;
    private String country;
    private ProxySource source;
}
