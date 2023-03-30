package by.gdev.alert.job.core.repository;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.DelayOrderNotification;

public interface DelayOrderNotificationRepository extends CrudRepository<DelayOrderNotification, Long> {

    void deleteAllByUser(AppUser user);
}
