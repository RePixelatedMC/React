package com.volmit.react.api.monitor;

import com.volmit.react.React;
import com.volmit.react.api.sampler.Sampler;
import com.volmit.react.legacyutil.ApproachingValue;
import com.volmit.react.legacyutil.M;
import com.volmit.react.legacyutil.TickedObject;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TickedMonitor extends TickedObject implements Monitor {
    protected final Map<Sampler, Double> samplers;
    protected final Map<Sampler, ApproachingValue> approachers;
    protected final Map<Sampler, Double> changers;
    @Getter
    protected final Map<Sampler, Boolean> visible;
    private final long awakeInterval;
    protected long sleepingRate;
    protected int sleepDelay;
    protected int currentSleepDelay;

    public TickedMonitor(String id, long interval) {
        super("monitor", id, interval);
        this.awakeInterval = interval;
        this.approachers = new HashMap<>();
        this.visible = new HashMap<>();
        this.sleepingRate = interval * 2;
        this.sleepDelay = 35;
        this.currentSleepDelay = 20;
        this.changers = new HashMap<>();
        this.samplers = new HashMap<>();
    }

    public void wakeUp() {
        currentSleepDelay = sleepDelay;
        setInterval(awakeInterval);
    }

    public void setVisible(Sampler sampler, boolean visible) {
        this.visible.put(sampler, visible);
    }

    public void setVisible(List<Sampler> sampler, boolean visible) {
        for (Sampler i : sampler) {
            setVisible(i, visible);
        }
    }

    public void clearVisibility() {
        for (Sampler i : visible.keySet()) {
            visible.put(i, false);
        }
    }

    public double getChanger(Sampler s) {
        Double d = changers.get(s);
        return d == null ? 0 : d;
    }

    @Override
    public void onTick() {
        for (Sampler i : new ArrayList<>(changers.keySet())) {
            changers.put(i, M.lerp(getChanger(i), 0, 0.1));
        }

        boolean flushable = false;

        for (Sampler i : visible.keySet()) {
            if (i == null) {
                continue;
            }
            if (visible.get(i) != null && visible.get(i)) {
                try {
                    synchronized (approachers) {
                        Double v = approachers.computeIfAbsent(i, k -> new ApproachingValue(0.25)).get(i.sample());
                        Double old = getSamplers().put(i, v);
                        double s = v;
                        if (old == null || old != s) {
                            changers.put(i, M.lerp(getChanger(i), 1, 0.333));
                            flushable = true;
                        }
                    }
                } catch (Throwable e) {
                    React.error("Failed to sample " + i.getId() + " for monitor " + getId());
                    e.printStackTrace();
                }
            }
        }

        currentSleepDelay = flushable ? sleepDelay : currentSleepDelay - 1;
        setInterval(currentSleepDelay <= 0 ? sleepingRate : awakeInterval);

        if (flushable || isAlwaysFlushing()) {
            flush();
        }
    }

    @Override
    public Map<Sampler, Double> getSamplers() {
        return samplers;
    }

    @Override
    public abstract void flush();
}