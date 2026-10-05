package com.hottalk.hottalkserver.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentDeletionScheduler {

    private final JdbcTemplate jdbcTemplate;

    public PaymentDeletionScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 기본 이벤트 이름과 SQL 템플릿({eventName} 플레이스홀더 사용)을 받아서
     * 이미 존재할 경우 접미사를 붙여 재시도하는 메서드.
     */
    private void createEventWithRetry(String baseEventName, String sqlTemplate) {
        int attempt = 0;
        while (true) {
            String eventName = baseEventName;
            if (attempt > 0) {
                eventName = baseEventName + "_" + attempt;
            }
            // 플레이스홀더 {eventName}를 실제 이벤트 이름으로 치환
            String sql = sqlTemplate.replace("{eventName}", eventName);
            try {
                jdbcTemplate.execute(sql);
                System.out.println("✅ 이벤트 생성됨: " + eventName);
                break; // 성공하면 반복 종료
            } catch (Exception ex) {
                if (ex.getMessage().contains("already exists")) {
                    attempt++;
                    System.out.println("이벤트 " + eventName + " 이미 존재함. 재시도: " + attempt);
                } else {
                    throw ex;
                }
            }
        }
    }

    public void schedulePaymentDeletion(Long userId, String provider) {
        // balance_history 테이블 삭제 예약 (5년 후)
        String balanceEventBase = "delete_balance_" + userId;
        String sqlBalanceTemplate = "CREATE EVENT {eventName} " +
                "ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 5 YEAR " +
                "DO DELETE FROM balance_history WHERE user_id = " + userId + ";";
        createEventWithRetry(balanceEventBase, sqlBalanceTemplate);

        // purchase_transaction 테이블 삭제 예약 (5년 후)
        String purchaseEventBase = "delete_purchase_" + userId;
        String sqlPurchaseTemplate;
        if (provider.equals("google")) {
            sqlPurchaseTemplate = "CREATE EVENT {eventName} " +
                    "ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 5 YEAR " +
                    "DO DELETE FROM purchase_transactions_android WHERE user_id = " + userId + ";";
        } else {
            sqlPurchaseTemplate = "CREATE EVENT {eventName} " +
                    "ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 5 YEAR " +
                    "DO DELETE FROM purchase_transactions_ios WHERE user_id = " + userId + ";";
        }
        createEventWithRetry(purchaseEventBase, sqlPurchaseTemplate);

        System.out.println("✅ 결제 내역 삭제 예약됨 (5년 후): userId = " + userId);
    }

    public void scheduleReportDeletion(Long userId) {
        String reporterEventBase = "delete_reporter_" + userId;
        String sqlReporterTemplate = "CREATE EVENT {eventName} " +
                "ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 3 YEAR " +
                "DO DELETE FROM report WHERE reporter_user_id = " + userId + ";";
        createEventWithRetry(reporterEventBase, sqlReporterTemplate);

        String reportChattingEventBase = "delete_report_chatting_" + userId;
        String sqlReportChattingTemplate = "CREATE EVENT {eventName} " +
                "ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 3 YEAR " +
                "DO DELETE FROM report_chatting WHERE reporter_user_id = " + userId + ";";
        createEventWithRetry(reportChattingEventBase, sqlReportChattingTemplate);

        System.out.println("✅ 신고 내역 삭제 예약됨 (3년 후): userId = " + userId);
    }

    public void scheduleCallCenterDeletion(Long userId) {
        String callCenterEventBase = "delete_call_center_" + userId;
        String sqlCallCenterTemplate = "CREATE EVENT {eventName} " +
                "ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 3 YEAR " +
                "DO DELETE FROM call_center WHERE caller_user_id = " + userId + ";";
        createEventWithRetry(callCenterEventBase, sqlCallCenterTemplate);

        System.out.println("✅ 문의 내역 삭제 예약됨 (3년 후): userId = " + userId);
    }

    public void scheduleUuidDeletion(String UUID, Long userId) {
        String uuidEventBase = "delete_UUID_" + userId;
        String sqlUUIDTemplate = "CREATE EVENT {eventName} " +
                "ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 1 DAY " +
                "DO DELETE FROM devices_uuid WHERE device_uuid = '" + UUID + "';";
        createEventWithRetry(uuidEventBase, sqlUUIDTemplate);

        System.out.println("✅ UUID 재가입 제한 내역 삭제 예약됨 (1일 후): UUID = " + UUID);
    }

    public void scheduleSanctionDeletion(Long userId) {
        String sanctionEventBase = "delete_sanction_" + userId;
        String sqlSanctionTemplate = "CREATE EVENT {eventName} " +
                "ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 6 MONTH " +
                "DO DELETE FROM sanctions WHERE user_id = '" + userId + "';";
        createEventWithRetry(sanctionEventBase, sqlSanctionTemplate);

        System.out.println("✅ 제재 내역 삭제 예약됨 (6개월 후): userId = " + userId);
    }

    public void scheduleTerms(String externalUserId) {
        // 외부 사용자 ID의 점(.)을 밑줄(_)로 치환해서 이벤트 이름에 사용
        String sanitizedUserId = externalUserId.replace(".", "_");
        String termsEventBase = "delete_terms_" + sanitizedUserId;
        String sqlTermsTemplate = "CREATE EVENT {eventName} " +
                "ON SCHEDULE AT CURRENT_TIMESTAMP + INTERVAL 1 YEAR " +
                "DO DELETE FROM user_terms WHERE external_user_id = '" + externalUserId + "';";
        createEventWithRetry(termsEventBase, sqlTermsTemplate);

        System.out.println("✅ 약관 동의 내역 삭제 예약됨 (1년 후): externalUserId = " + externalUserId);
    }
}
