package by.gdev.alert.job.core.repository;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.UserAlertTime;

public interface UserAlertTimeRepository extends CrudRepository<UserAlertTime, Long> {

}
