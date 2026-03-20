-- TalkDream Messaging FAQ Seed
INSERT INTO cs_faq (cs_faq_id, category, question, answer, tags, use_yn, created_at, updated_at) VALUES 
('FAQ_TLK_001', '알림톡', '알림톡 발송에 실패하면 어떻게 되나요?', '알림톡 발송 실패 시, 자동으로 SMS 또는 LMS로 대체 발송되도록 하는 [부달(메시지 보상)] 기능을 설정할 수 있습니다. 포털 내 [발송 설정] 메뉴에서 부달 여부를 체크하고 대체 문구를 등록해 주세요.', '알림톡,발송실패,부달,SMS대체', 'Y', NOW(), NOW()),
('FAQ_TLK_002', 'LMS/MMS', 'LMS와 MMS의 차이점과 최대 바이트는 무엇인가요?', 'SMS는 단문(90byte), LMS는 장문(한글 1000자/2000byte), MMS는 이미지가 포함된 메시지입니다. 이미지는 JPG/PNG 형식을 지원하며 최대 3장까지 첨부 가능합니다.', 'LMS,MMS,바이트,규격', 'Y', NOW(), NOW()),
('FAQ_TLK_003', 'RCS', 'RCS 브랜치 등록 절차가 궁금합니다.', 'RCS 발송을 위해서는 먼저 [브랜드 등록]이 필요합니다. 업체 정보를 등록하고 심사를 거쳐 승인된 후 [템플릿]을 생성하여 발송할 수 있습니다. 심사 기간은 일반적으로 영업일 기준 2~3일이 소요됩니다.', 'RCS,브랜드등록,심사,승인', 'Y', NOW(), NOW()),
('FAQ_TLK_004', '공통', '메시지 발송 결과의 "수신거부"는 무엇을 의미하나요?', '수신인이 080 수신거부 번호를 통해 해당 업체의 메시지 수신을 거부한 상태입니다. 시스템에서 자동으로 필터링되어 발송되지 않으며, 차단 목록은 [수신거부 관리] 메뉴에서 확인할 수 있습니다.', '수신거부,080,차단', 'Y', NOW(), NOW());

-- TalkDream Inbound Data Seed (Smarter processing history for AI learning)
INSERT INTO cs_inbound_data (cs_inbound_id, source, external_ref_id, raw_payload, resolved_payload, processing_history, status, received_at) VALUES
('INB_TLK_001', 'PORTAL', 'CASE-2024-001', '알림톡 템플릿 심사가 왜 자꾸 반려되나요?', '템플릿 내에 변수가 너무 많거나, 홍보성 문구가 포함되어 반려된 사례입니다. 가이드에 따라 정보성 문구로 수정 후 재승인되었습니다.', '2024-03-10: 템플릿 반려 확인\n2024-03-11: 카카오 가이드 준수 여부 체크\n2024-03-12: 문구 수정 후 재승인 완료', 'PROCESSED', NOW()),
('INB_TLK_002', 'PORTAL', 'CASE-2024-002', 'RCS 발송 시 이미지가 안 나와요.', 'MMS와 달리 RCS는 이미지 규격(가로 600px 이상 권장)과 파일 포맷이 엄격합니다. 규격에 맞는 이미지로 교체하여 해결되었습니다.', '2024-03-15: 오류 로그 확인 (규격 미달)\n2024-03-16: 고객사 재규격 가이드 전달\n2024-03-17: 이미지 교체 후 정상 발송 확인', 'PROCESSED', NOW());
