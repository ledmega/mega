package led.mega.repository;

import led.mega.entity.CsFaq;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CsFaqRepository extends ReactiveCrudRepository<CsFaq, String> {
    
    Flux<CsFaq> findByUseYnOrderByCreatedAtDesc(String useYn);
    
    @Query("SELECT * FROM cs_faq WHERE (question LIKE CONCAT('%', :keyword, '%') OR tags LIKE CONCAT('%', :keyword, '%')) AND use_yn = 'Y' ORDER BY created_at DESC")
    Flux<CsFaq> searchFaq(String keyword);
}
