package io.github.luminion.velo.spi.func;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmailMaskerTests {
    @Test
    void shouldMaskCompleteLocalPartRegardlessOfDomainOrLocalPartPunctuation() {
        EmailMasker masker = new EmailMasker();
        assertThat(masker.apply("alice@EXAMPLE.COM")).isEqualTo("a****@EXAMPLE.COM");
        assertThat(masker.apply("alice@example-domain.com")).isEqualTo("a****@example-domain.com");
        assertThat(masker.apply("alice.smith@example.com")).isEqualTo("a****@example.com");
        assertThat(masker.apply("alice+tag@example.com")).isEqualTo("a****@example.com");
        assertThat(masker.apply("a@example.com")).isEqualTo("a****@example.com");
        assertThat(masker.apply("invalid")).isEqualTo("****");
        assertThat(masker.apply(null)).isNull();
    }
}
