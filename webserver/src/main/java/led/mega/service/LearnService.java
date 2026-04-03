package led.mega.service;

import led.mega.entity.CsFaq;
import led.mega.repository.CsFaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class LearnService {

    private final CsFaqRepository faqRepository;

    /**
     * 자바 리액티브 학습용 데이터 조회 (카테고리가 'JAVA_REACTIVE'인 것들)
     */
    public Flux<CsFaq> getReactiveLearningContents() {
        return faqRepository.findAllByUseYnOrderByCreatedAtDesc("Y")
                .filter(faq -> "JAVA_REACTIVE".equals(faq.getCategory()));
    }

    /**
     * 키워드로 검색
     */
    public Flux<CsFaq> searchReactiveLearning(String keyword) {
        return faqRepository.searchFaq(keyword)
                .filter(faq -> "JAVA_REACTIVE".equals(faq.getCategory()));
    }
}
