package led.mega.service;

import led.mega.entity.Menu;
import led.mega.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    /**
     * 앱 시작 시 누락된 메뉴 데이터를 자동으로 삽입
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public Mono<Void> initMenus() {
        List<Menu> defaultMenus = new ArrayList<>();
        defaultMenus.add(Menu.builder().name("홈").url("/dashboard").icon("fa-home").sortOrder(1).build());
        defaultMenus.add(Menu.builder().name("에이전트").url("/agents").icon("fa-server").sortOrder(2).build());
        defaultMenus.add(Menu.builder().name("배치 스케줄러").url("/scheduler").icon("fa-clock").sortOrder(3).build());
        defaultMenus.add(Menu.builder().name("서비스 관리").url("/services").icon("fa-cogs").sortOrder(4).build());
        defaultMenus.add(Menu.builder().name("사용자 관리").url("/members").icon("fa-users").sortOrder(5).requiredRole("ROLE_ADMIN").build());
        defaultMenus.add(Menu.builder().name("권한 관리").url("/authority").icon("fa-user-shield").sortOrder(6).requiredRole("ROLE_ADMIN").build());
        defaultMenus.add(Menu.builder().name("메뉴 관리").url("/menu").icon("fa-bars").sortOrder(7).requiredRole("ROLE_ADMIN").build());

        return Flux.fromIterable(defaultMenus)
                .flatMap(menu -> menuRepository.findByUrl(menu.getUrl())
                        .switchIfEmpty(Mono.defer(() -> {
                            log.info("누락된 메뉴를 추가합니다: {} ({})", menu.getName(), menu.getUrl());
                            return menuRepository.save(menu);
                        }))
                )
                .then()
                .doOnSuccess(v -> log.info("메뉴 초기화/동기화 완료"));
    }

    public Flux<Menu> getAllMenus() {
        return menuRepository.findAllByOrderBySortOrder();
    }

    public Flux<Menu> getEnabledMenus() {
        return menuRepository.findAllByEnabledOrderBySortOrder();
    }

    @Transactional
    public Mono<Menu> saveMenu(Menu menu) {
        return menuRepository.save(menu);
    }

    @Transactional
    public Mono<Void> deleteMenu(Long id) {
        return menuRepository.deleteById(id);
    }

    public Mono<Menu> getMenu(Long id) {
        return menuRepository.findById(id);
    }
}
