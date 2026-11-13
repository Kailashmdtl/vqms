package com.sutra.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void blankToNullTrimsRealText() {
        assertThat(Text.blankToNull("  Axiom Auto  ")).isEqualTo("Axiom Auto");
    }

    @Test
    void blankToNullTurnsNullEmptyAndWhitespaceIntoNull() {
        assertThat(Text.blankToNull(null)).isNull();
        assertThat(Text.blankToNull("")).isNull();
        assertThat(Text.blankToNull(" \t\n ")).isNull();
    }
}
