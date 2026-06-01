package com.jes.devlearn.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HtmlSanitizer XSS 방어")
class HtmlSanitizerTest {

    @Test
    @DisplayName("script 블록 제거")
    void removes_script() {
        String out = HtmlSanitizer.sanitize("안녕<script>alert('x')</script>하세요");
        assertThat(out).doesNotContainIgnoringCase("<script");
        assertThat(out).contains("안녕").contains("하세요");
    }

    @Test
    @DisplayName("인라인 이벤트 핸들러 무력화")
    void neutralizes_event_handlers() {
        String out = HtmlSanitizer.sanitize("<img src=x onerror=alert(1)>");
        assertThat(out).doesNotContainIgnoringCase("onerror=");
    }

    @Test
    @DisplayName("javascript: 스킴 제거 + 꺾쇠 이스케이프")
    void escapes_brackets_and_scheme() {
        String out = HtmlSanitizer.sanitize("<a href=\"javascript:evil()\">click</a>");
        assertThat(out).doesNotContain("<").doesNotContain(">");
        assertThat(out).doesNotContainIgnoringCase("javascript:");
    }

    @Test
    @DisplayName("정상 텍스트는 보존")
    void keeps_plain_text() {
        String out = HtmlSanitizer.sanitize("질문이 있습니다. 1 + 1 = 2 인가요?");
        assertThat(out).isEqualTo("질문이 있습니다. 1 + 1 = 2 인가요?");
    }

    @Test
    @DisplayName("null 입력은 null 반환")
    void null_passthrough() {
        assertThat(HtmlSanitizer.sanitize(null)).isNull();
    }
}
