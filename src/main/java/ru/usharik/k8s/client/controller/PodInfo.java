package ru.usharik.k8s.client.controller;

import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1Pod;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Minutes;

import java.util.List;

public class PodInfo {

    private final V1Pod v1Pod;

    public PodInfo(V1Pod v1Pod) {
        this.v1Pod = v1Pod;
    }

    public String getName() {
        return v1Pod.getMetadata().getName();
    }

    public String getStatus() {
        return v1Pod.getStatus().getPhase();
    }

    public DateTime getStartTime() {
        return v1Pod.getStatus().getStartTime();
    }

    public int getMinutesFromStart() {
        if (getStartTime() != null) {
            return Minutes.minutesBetween(getStartTime(), Instant.now()).getMinutes();
        }
        return 0;
    }

    public String getTenantName() {
        List<V1Container> containers = v1Pod.getSpec().getContainers();
        try {
            return containers.get(0).getEnv().stream()
                    .filter(env -> env.getName().equals("TENANT_NAME"))
                    .map(V1EnvVar::getValue)
                    .findFirst()
                    .orElse("");
        } catch (Exception ex) {
            return "";
        }
    }

    public V1Pod getV1Pod() {
        return v1Pod;
    }
}
