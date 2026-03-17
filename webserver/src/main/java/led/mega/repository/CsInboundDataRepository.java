package led.mega.repository;

import led.mega.entity.CsInboundData;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CsInboundDataRepository extends ReactiveCrudRepository<CsInboundData, String> {
    
    Flux<CsInboundData> findByStatus(String status);
    
    Flux<CsInboundData> findBySource(String source);
}
