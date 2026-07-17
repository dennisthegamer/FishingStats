package de.dennisthegamer.fishingstats.tracker;

import de.dennisthegamer.fishingstats.FishingStatsClient;
import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.CatchRecord;
import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.data.FishingSession;

/**
 * Session lifecycle: tracking starts paused (like TradeTracker) - the player enables it
 * with the session keybind. While running, a session begins with the first cast and ends
 * after the configured minutes without a cast. The idle tail is not counted - endTime is
 * set to the last activity, not to the moment the timeout fires. The ESC menu pauses
 * tracking automatically and resumes it once the player is back in-game; paused time
 * never counts towards the session duration.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private FishingSession active;
    private long lastActivityMs;
    private boolean paused = true;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public FishingSession getActiveSession() {
        return active;
    }

    public boolean isPaused() {
        return paused;
    }

    public void pause() {
        if (paused) return;
        paused = true;
        if (active != null) {
            active.pauseStartMs = System.currentTimeMillis();
            FishingDataStore.getInstance().saveToDisk();
        }
    }

    public void resume() {
        if (!paused) return;
        paused = false;
        long now = System.currentTimeMillis();
        if (active != null && active.pauseStartMs > 0) {
            active.pausedMs += now - active.pauseStartMs;
            active.pauseStartMs = 0;
        }
        // Keep the inactivity timeout from firing right after a long pause
        lastActivityMs = now;
    }

    public void togglePause() {
        if (paused) {
            resume();
        } else {
            pause();
        }
    }

    private long timeoutMs() {
        return FishingStatsConfig.getInstance().sessionSplitMinutes * 60_000L;
    }

    /**
     * Legt eine Session an, falls keine läuft. Der Toggle-Key verspricht "Session starten" und die
     * Beitrittsnachricht sagt "Drücke [X] um die Session zu starten" — er legte aber nur die Pause ab,
     * sodass "Session gestartet!" im Chat stand, während gar keine Session lief.
     * Bewusst NICHT in resume(): das wird auch beim Schliessen des ESC-Menüs gerufen und darf dort
     * keine Session anlegen.
     */
    public void startIfNone(long now, String dimension) {
        if (active != null) return;
        active = FishingDataStore.getInstance().startSession(now, dimension);
        FishingStatsClient.LOGGER.info("Fishing session #{} started (session key)", active.id);
        FishingDataStore.getInstance().saveToDisk();
    }

    public void onCast(long now, String dimension) {
        if (paused) return;
        if (active != null && now - lastActivityMs > timeoutMs()) {
            endSession();
        }
        if (active == null) {
            active = FishingDataStore.getInstance().startSession(now, dimension);
            FishingStatsClient.LOGGER.info("Fishing session #{} started", active.id);
        }
        active.totalCasts++;
        lastActivityMs = now;
        FishingDataStore.getInstance().saveToDisk();
    }

    public void onCatch(CatchRecord record) {
        if (paused || active == null) return;
        active.catches.add(record);
        active.updatePrimaryBiome();
        lastActivityMs = Math.max(lastActivityMs, record.timestamp);
        FishingDataStore.getInstance().saveToDisk();
    }

    /** Called every client tick; ends the session once the inactivity limit is reached. */
    public void tick(long now) {
        if (paused) return;
        if (active != null && now - lastActivityMs > timeoutMs()) {
            endSession();
        }
    }

    /**
     * Adopts a still-open session on world join (persistSessions). The session was left
     * with pauseStartMs set, so the whole offline gap is booked as paused time on resume.
     * Returns true if a session was restored.
     */
    public boolean restoreSession() {
        paused = true;
        if (active == null) {
            FishingSession newest = null;
            for (FishingSession s : FishingDataStore.getInstance().getSessions()) {
                if (s.isActive() && (newest == null || s.startTime > newest.startTime)) {
                    newest = s;
                }
            }
            active = newest;
        }
        if (active == null) return false;
        if (active.pauseStartMs <= 0) {
            // Crash while fishing: no clean pause was recorded - treat everything
            // after the last activity as pause
            long last = active.startTime;
            for (CatchRecord c : active.catches) last = Math.max(last, c.timestamp);
            active.pauseStartMs = last;
        }
        FishingStatsClient.LOGGER.info("Fishing session #{} restored (paused)", active.id);
        return true;
    }

    /**
     * Discards the active session entirely (its casts and catches are deleted) and
     * pauses tracking again - the FishingStats window's "[R] reset" action.
     */
    public void resetSession() {
        paused = true;
        if (active == null) return;
        FishingStatsClient.LOGGER.info("Fishing session #{} reset (discarded)", active.id);
        FishingDataStore.getInstance().removeSession(active);
        active = null;
    }

    /** Ends the active session (inactivity, world leave or disconnect). */
    public void endSession() {
        if (active == null) return;
        if (active.pauseStartMs > 0) {
            // Session ends while paused - the open pause segment never counted anyway
            active.pauseStartMs = 0;
        }
        active.endTime = lastActivityMs;
        FishingStatsClient.LOGGER.info("Fishing session #{} ended ({} casts, {} catches)",
                active.id, active.totalCasts, active.catches.size());
        active = null;
        FishingDataStore.getInstance().saveToDisk();
    }
}
