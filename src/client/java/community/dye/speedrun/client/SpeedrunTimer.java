package community.dye.speedrun.client;

public final class SpeedrunTimer {
    private long activeNs;
    private long lastNano;
    private boolean running;
    private boolean paused;

    public synchronized void start(long existingActiveMs, boolean pausedNow) {
        this.activeNs = Math.max(0, existingActiveMs) * 1_000_000L;
        this.lastNano = System.nanoTime();
        this.running = true;
        this.paused = pausedNow;
    }

    public synchronized long sample(boolean pausedNow) {
        if (!running) return activeNs / 1_000_000L;

        long now = System.nanoTime();
        long deltaNs = Math.max(0, now - lastNano);
        if (!paused) activeNs += deltaNs;
        lastNano = now;
        paused = pausedNow;
        return activeNs / 1_000_000L;
    }

    public synchronized long stop(boolean pausedNow) {
        long value = sample(pausedNow);
        running = false;
        return value;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    /**
     * Returns the last timer value without sampling System.nanoTime().
     * HUD rendering can happen hundreds of times per second, so it must not
     * advance/reset the timing sample point used by the 20 TPS client tick.
     */
    public synchronized long value() {
        return activeNs / 1_000_000L;
    }
}
