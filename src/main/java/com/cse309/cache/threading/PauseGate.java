package com.cse309.cache.threading;

/**
 * Thread synchronization gate enabling cooperative pause and resume.
 * Utilizes synchronized, wait(), and notifyAll() (Unit 3).
 */
public final class PauseGate {

    private boolean paused = false;

    public synchronized void await() throws InterruptedException {
        while (paused) {
            wait();
        }
    }

    public synchronized void pause() {
        if (!paused) {
            paused = true;
            notifyAll();
        }
    }

    public synchronized void resume() {
        if (paused) {
            paused = false;
            notifyAll();
        }
    }

    public synchronized boolean isPaused() {
        return paused;
    }
}
