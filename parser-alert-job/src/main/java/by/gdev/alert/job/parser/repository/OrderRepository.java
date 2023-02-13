package by.gdev.alert.job.parser.repository;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.Order;

public interface OrderRepository extends CrudRepository<Order, Long>{

}