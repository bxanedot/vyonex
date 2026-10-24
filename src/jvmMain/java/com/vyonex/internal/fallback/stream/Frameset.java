// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import java.io.Serializable;
import java.util.List;

public final class Frameset implements Serializable {
    private final List<String> urls;
    private final int frameWidth;
    private final int frameHeight;
    private final int totalCount;
    private final int durationPerFrame;
    private final int framesPerPageX;
    private final int framesPerPageY;

    public Frameset(final List<String> urls, final int frameWidth, final int frameHeight,
                    final int totalCount, final int durationPerFrame,
                    final int framesPerPageX, final int framesPerPageY) {
        this.urls = urls;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.totalCount = totalCount;
        this.durationPerFrame = durationPerFrame;
        this.framesPerPageX = framesPerPageX;
        this.framesPerPageY = framesPerPageY;
    }

    public List<String> getUrls() { return urls; }
    public int getFrameWidth() { return frameWidth; }
    public int getFrameHeight() { return frameHeight; }
    public int getTotalCount() { return totalCount; }
    public int getDurationPerFrame() { return durationPerFrame; }
    public int getFramesPerPageX() { return framesPerPageX; }
    public int getFramesPerPageY() { return framesPerPageY; }

    public int[] getFrameBoundsAt(final long position) {
        if (position < 0 || position > (((long) totalCount + 1) * durationPerFrame)) {
            return new int[] {0, 0, 0, frameWidth, frameHeight};
        }

        final int framesPerPage = framesPerPageX * framesPerPageY;
        final int frame = Math.min((int) (position / durationPerFrame), totalCount);
        final int frameOnPage = frame % framesPerPage;
        final int row = Math.floorDiv(frameOnPage, framesPerPageX);
        final int column = frameOnPage % framesPerPageX;
        final int left = column * frameWidth;
        final int top = row * frameHeight;

        return new int[] {
                Math.floorDiv(frame, framesPerPage), left, top,
                left + frameWidth, top + frameHeight
        };
    }
}
