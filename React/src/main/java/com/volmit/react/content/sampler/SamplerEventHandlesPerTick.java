package com.volmit.react.content.sampler;

import art.arcane.chrono.RollingSequence;
import com.volmit.react.React;
import com.volmit.react.api.sampler.ReactCachedSampler;
import com.volmit.react.core.controller.EventController;
import com.volmit.react.util.format.Form;
import org.bukkit.event.Listener;

public class SamplerEventHandlesPerTick extends ReactCachedSampler implements Listener {
    public static final String ID = "event-handles-per-tick";
    private EventController eventController;
    private RollingSequence r = new RollingSequence(36);

    public SamplerEventHandlesPerTick() {
        super(ID, 50);
    }

    @Override
    public double onSample() {
        r.put(eventController.getCalls());
        return r.getAverage();
    }

    @Override
    public void start() {
        eventController = React.instance.getEventController();
    }

    @Override
    public String formattedValue(double t) {
        return Form.f(Math.round(t));
    }

    @Override
    public String formattedSuffix(double t) {
        return "EVENT/t";
    }
}
