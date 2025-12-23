package by.gdev.alert.job.parser.proxy.repository;

import by.gdev.alert.job.parser.proxy.db.ProxyInfo;
import by.gdev.alert.job.parser.proxy.db.ProxyState;
import by.gdev.alert.job.parser.proxy.db.ProxyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProxyInfoRepository extends JpaRepository<ProxyInfo, Long> {

    // найти все прокси по состоянию
    List<ProxyInfo> findByState(ProxyState state);

    // найти все прокси по типу
    List<ProxyInfo> findByType(ProxyType type);

    // найти активные прокси определённого типа
    List<ProxyInfo> findByTypeAndState(ProxyType type, ProxyState state);

    List<ProxyInfo> findByStateIn(List<ProxyState> states);
}
