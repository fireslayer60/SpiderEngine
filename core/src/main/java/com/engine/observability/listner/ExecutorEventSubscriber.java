package com.engine.observability.listner;

import com.engine.observability.stats.ExecutorEvent;

public interface ExecutorEventSubscriber {
    void onEvent(ExecutorEvent event);
}