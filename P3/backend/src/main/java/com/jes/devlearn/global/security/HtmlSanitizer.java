package com.jes.devlearn.global.security;

/**
 * 저장 전 사용자 입력의 XSS 방어용 경량 새니타이저(외부 의존성 없음).
 * 학습용 단순화: 위험 태그/스크립트 패턴을 제거하고 핵심 특수문자를 이스케이프한다.
 * 프론트(React)도 기본 이스케이프하므로 다층 방어(defense-in-depth) 목적.
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {}

    public static String sanitize(String input) {
        if (input == null) return null;
        String s = input;
        // <script>...</script> 블록 제거 (대소문자 무시, 개행 포함)
        s = s.replaceAll("(?is)<\\s*script.*?>.*?<\\s*/\\s*script\\s*>", "");
        // 인라인 이벤트 핸들러 (onerror=, onclick= 등) 무력화
        s = s.replaceAll("(?i)\\son\\w+\\s*=", " data-blocked=");
        // javascript: 스킴 제거
        s = s.replaceAll("(?i)javascript:", "");
        // 잔여 태그의 꺾쇠 이스케이프
        s = s.replace("<", "&lt;").replace(">", "&gt;");
        return s;
    }
}
