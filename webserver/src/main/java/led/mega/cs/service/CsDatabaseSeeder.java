package led.mega.cs.service;

import led.mega.entity.CsFaq;
import led.mega.entity.CsInboundData;
import led.mega.repository.CsFaqRepository;
import led.mega.repository.CsInboundDataRepository;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsDatabaseSeeder {

    private final CsFaqRepository faqRepository;
    private final CsInboundDataRepository inboundDataRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        faqRepository.count()
                .filter(count -> count == 0)
                .flatMapMany(count -> {
                    log.info("[CS-SEED] FAQ 테이블이 비어있어 초기 데이터를 주입합니다 (TalkDream)...");
                    return Flux.just(
                            createFaq("알림톡", "알림톡 발송에 실패하면 어떻게 되나요?", "알림톡 발송 실패 시, 자동으로 SMS 또는 LMS로 대체 발송되도록 하는 [부달(메시지 보상)] 기능을 설정할 수 있습니다. 포털 내 [발송 설정] 메뉴에서 부달 여부를 체크하고 대체 문구를 등록해 주세요.", "알림톡,발송실패,부달,SMS대체"),
                            createFaq("LMS/MMS", "LMS와 MMS의 차이점과 최대 바이트는 무엇인가요?", "SMS는 단문(90byte), LMS는 장문(한글 1000자/2000byte), MMS는 이미지가 포함된 메시지입니다. 이미지는 JPG/PNG 형식을 지원하며 최대 3장까지 첨부 가능합니다.", "LMS,MMS,바이트,규격"),
                            createFaq("RCS", "RCS 브랜드 등록 절차가 궁금합니다.", "RCS 발송을 위해서는 먼저 [브랜드 등록]이 필요합니다. 업체 정보를 등록하고 심사를 거쳐 승인된 후 [템플릿]을 생성하여 발송할 수 있습니다. 심사 기간은 일반적으로 영업일 기준 2~3일이 소요됩니다.", "RCS,브랜드등록,심사,승인")
                    ).flatMap(faqRepository::save);
                })
                .subscribe();

        inboundDataRepository.count()
                .filter(count -> count == 0)
                .flatMapMany(count -> {
                    log.info("[CS-SEED] InboundData 테이블이 비어있어 과거 사례를 주입합니다...");
                    return Flux.just(
                            createInbound("CASE-001", "알림톡 템플릿 심사가 왜 자꾸 반려되나요?", "홍보성 문구 제외 후 재승인 완료", "2024-03-10: 템플릿 반려 확인\n2024-03-12: 문구 수정 후 재승인 완료"),
                            createInbound("CASE-002", "RCS 발송 시 이미지가 안 나와요.", "규격에 맞는 이미지(600px 이상)로 교체하여 해결되었습니다.", "2024-03-15: 이미지 규격 미달 확인\n2024-03-17: 이미지 교격 가이드 전달 후 해결")
                    ).flatMap(inboundDataRepository::save);
                })
                .subscribe();
    }

    private CsFaq createFaq(String cat, String q, String a, String t) {
        CsFaq faq = new CsFaq();
        faq.setCsFaqId(IdGenerator.generate(IdGenerator.CS_FAQ));
        faq.setCategory(cat);
        faq.setQuestion(q);
        faq.setAnswer(a);
        faq.setTags(t);
        faq.setUseYn("Y");
        faq.setCreatedAt(LocalDateTime.now());
        faq.setUpdatedAt(LocalDateTime.now());
        faq.setNew(true);
        return faq;
    }

    private CsInboundData createInbound(String ref, String raw, String resolved, String history) {
        return CsInboundData.builder()
                .csInboundId(IdGenerator.generate(IdGenerator.CS_INBOUND))
                .source("PORTAL")
                .externalRefId(ref)
                .rawPayload(raw)
                .resolvedPayload(resolved)
                .processingHistory(history)
                .status("PROCESSED")
                .receivedAt(LocalDateTime.now())
                .isNew(true)
                .build();
    }
}
