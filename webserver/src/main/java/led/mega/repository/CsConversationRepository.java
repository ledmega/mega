package led.mega.repository;

import led.mega.entity.CsConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CsConversationRepository extends ReactiveCrudRepository<CsConversation, String> {
    
    Mono<CsConversation> findByExternalIdAndStatusNot(String externalId, String status);
    
    Mono<CsConversation> findByExternalId(String externalId);

    Flux<CsConversation> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
