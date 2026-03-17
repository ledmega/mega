package led.mega.repository;

import led.mega.entity.Menu;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MenuRepository extends ReactiveCrudRepository<Menu, String> {

    @Query("SELECT * FROM menu WHERE is_enabled = TRUE ORDER BY sort_order ASC")
    Flux<Menu> findAllByEnabledOrderBySortOrder();

    @Query("SELECT * FROM menu ORDER BY sort_order ASC")
    Flux<Menu> findAllByOrderBySortOrder();

    Mono<Menu> findByUrl(String url);
}
