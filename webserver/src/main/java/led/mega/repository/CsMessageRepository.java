package led.mega.repository;

import led.mega.entity.CsMessage;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CsMessageRepository extends ReactiveCrudRepository<CsMessage, String> {
    
    Flux<CsMessage> findByCsConvIdOrderByCreatedAtAsc(String csConvId);
}
