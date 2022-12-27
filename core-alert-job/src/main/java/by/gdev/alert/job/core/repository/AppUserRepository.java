package by.gdev.alert.job.core.repository;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.AppUser;

public interface AppUserRepository extends CrudRepository<AppUser, Long> {

}
