package by.gdev.alert.job.parser.repository;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.OrderLinks;

public interface OrderLinksRepository extends CrudRepository<OrderLinks, Long> {
	
	boolean existsByLinks(String links);
}
