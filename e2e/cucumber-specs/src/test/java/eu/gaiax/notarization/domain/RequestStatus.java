package eu.gaiax.notarization.domain;

import java.util.Map;

import io.cucumber.java.ParameterType;

public enum RequestStatus {
    Editable,
    Submittable,
    MarkedReady,
    WorkInProgress;

    private static Map<String,RequestStatus> aliases = Map.ofEntries(
            Map.entry("editable", Editable),
            Map.entry("EDITABLE", Editable),
            Map.entry("submittable", Submittable),
            Map.entry("SUBMITTABLE", Submittable),
            Map.entry("ready", MarkedReady),
            Map.entry("READY", MarkedReady),
            Map.entry("marked ready", MarkedReady),
            Map.entry("MARKED_READY", MarkedReady),
            Map.entry("in progress", WorkInProgress),
            Map.entry("IN_PROGRESS", WorkInProgress),
            Map.entry("work in progress", WorkInProgress),
            Map.entry("WORK_IN_PROGRESS", WorkInProgress)
    );

    @ParameterType("editable|EDITABLE|submittable|SUBMITTABLE|ready|READY|marked ready|MARKED_READY|in progress|IN_PROGRESS|work in progress|WORK_IN_PROGRESS")
    public static RequestStatus status(String status) {
        var found = RequestStatus.valueOf(status);
        if (found != null) {
            return found;
        }
        found = aliases.get(status);
        if (found != null) {
            return found;
        }
        throw new IllegalArgumentException("The following value does not represent a valid status: " + status);
    }
}
