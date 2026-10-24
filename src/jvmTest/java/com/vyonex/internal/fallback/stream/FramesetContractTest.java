// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class FramesetContractTest {
    @Test
    void locatesFramesAcrossNonSquareSpritePages() {
        final Frameset frames = new Frameset(
                List.of("sprite-0", "sprite-1"), 10, 20, 11, 1_000, 3, 2);

        assertArrayEquals(new int[] {0, 20, 0, 30, 20}, frames.getFrameBoundsAt(2_000));
        assertArrayEquals(new int[] {1, 0, 0, 10, 20}, frames.getFrameBoundsAt(6_000));
    }
}
