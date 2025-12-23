package by.gdev.alert.job.parser.proxy.db;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proxies")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProxyInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String ip;

    @Column(nullable = false)
    private int port;

    private String username;
    private String password;

    @Enumerated(EnumType.STRING)
    private ProxyType type;

    @Enumerated(EnumType.STRING)
    private ProxyState state = ProxyState.NEW;

    private int errorCount;
    private LocalDateTime lastChecked;

    public boolean isActive() {
        return this.state == ProxyState.ACTIVE || this.state == ProxyState.WARMING_UP;
    }

}

