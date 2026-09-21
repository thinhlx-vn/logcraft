package vn.logcraft.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrokCompilerTest {
    @Test
    void extractsFields() {
        GrokCompiler.GrokMatch result = GrokCompiler.match(
                "%{IP:client.ip} %{WORD:http.method} %{INT:http.status_code}",
                "10.20.30.40 GET 200"
        );
        assertTrue(result.matched());
        assertEquals("10.20.30.40", result.fields().get("client.ip"));
        assertEquals("GET", result.fields().get("http.method"));
        assertEquals("200", result.fields().get("http.status_code"));
    }

    @Test
    void rejectsUnknownPattern() {
        assertThrows(IllegalArgumentException.class, () -> GrokCompiler.compile("%{DOES_NOT_EXIST:value}"));
    }
}
