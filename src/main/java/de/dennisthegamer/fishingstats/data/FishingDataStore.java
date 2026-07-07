package de.dennisthegamer.fishingstats.data;

import de.dennisthegamer.fishingstats.FishingStatsClient;
import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persists all fishing sessions to fishingstats_sessions.json in the config directory. */
public class FishingDataStore {

    private static final FishingDataStore INSTANCE = new FishingDataStore();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORE_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("fishingstats_sessions.json");

    private final List<FishingSession> sessions = new ArrayList<>();
    private boolean loaded = false;

    private FishingDataStore() {}

    public static FishingDataStore getInstance() {
        return INSTANCE;
    }

    /** All sessions, newest first. */
    public List<FishingSession> getSessionsNewestFirst() {
        List<FishingSession> copy = new ArrayList<>(sessions);
        copy.sort((a, b) -> Long.compare(b.startTime, a.startTime));
        return copy;
    }

    public List<FishingSession> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    public FishingSession startSession(long now, String dimension) {
        FishingSession session = new FishingSession();
        session.id = nextId();
        session.startTime = now;
        session.dimension = dimension;
        sessions.add(session);
        return session;
    }

    /** Removes a session permanently, e.g. when the active session is reset. */
    public void removeSession(FishingSession session) {
        sessions.remove(session);
        saveToDisk();
    }

    private int nextId() {
        int max = 0;
        for (FishingSession s : sessions) max = Math.max(max, s.id);
        return max + 1;
    }

    public void saveToDisk() {
        try {
            Files.writeString(STORE_FILE, GSON.toJson(sessions));
        } catch (IOException e) {
            FishingStatsClient.LOGGER.error("Failed to save fishing session store", e);
        }
    }

    public void loadFromDisk() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(STORE_FILE)) return;
        try {
            String json = Files.readString(STORE_FILE);
            Type type = new TypeToken<List<FishingSession>>() {}.getType();
            List<FishingSession> data = GSON.fromJson(json, type);
            if (data == null) return;
            sessions.clear();
            for (FishingSession s : data) {
                if (s != null) {
                    if (s.catches == null) s.catches = new ArrayList<>();
                    // A session left open is closed at its last known activity - unless
                    // persistSessions keeps it restorable (SessionManager.restoreSession)
                    if (s.endTime == 0 && !FishingStatsConfig.getInstance().persistSessions) {
                        long lastActivity = s.startTime;
                        for (CatchRecord c : s.catches) lastActivity = Math.max(lastActivity, c.timestamp);
                        s.endTime = lastActivity;
                    }
                    sessions.add(s);
                }
            }
        } catch (Exception e) {
            FishingStatsClient.LOGGER.error("Failed to load fishing session store", e);
        }
    }
}
