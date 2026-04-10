package by.gdev.common.model.proxy;

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
}
