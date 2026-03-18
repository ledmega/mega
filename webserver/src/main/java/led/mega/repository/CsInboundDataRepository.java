package led.mega.repository;

import led.mega.entity.CsInboundData;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CsInboundDataRepository extends ReactiveCrudRepository<CsInboundData, String> {
    
    Flux<CsInboundData> findByStatus(String status);
    
    Flux<CsInboundData> findBySource(String source);
    
    Flux<CsInboundData> findAllByOrderByReceivedAtDesc();
    
    @Query("SELECT * FROM cs_inbound_data WHERE raw_payload LIKE CONCAT('%', :keyword, '%') AND status = 'PROCESSED' LIMIT 10")
    Flux<CsInboundData> searchInbound(String keyword);
}
