package com.zuehlke.securesoftwaredevelopment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ServiceHistorySearchRequest {
    private final String carModel;
    private final String serviceName;
    private final String partName;
    private final String technician;
    private final boolean showPerformedServices;
    private final boolean showUsedParts;

    @JsonCreator
    public ServiceHistorySearchRequest(@JsonProperty("carModel") String carModel,
                                       @JsonProperty("serviceName") String serviceName,
                                       @JsonProperty("partName") String partName,
                                       @JsonProperty("technician") String technician,
                                       @JsonProperty("showPerformedServices") boolean showPerformedServices,
                                       @JsonProperty("showUsedParts") boolean showUsedParts) {
        this.carModel = carModel;
        this.serviceName = serviceName;
        this.partName = partName;
        this.technician = technician;
        this.showPerformedServices = showPerformedServices;
        this.showUsedParts = showUsedParts;
    }

    public String carModel() {
        return carModel;
    }

    public String serviceName() {
        return serviceName;
    }

    public String partName() {
        return partName;
    }

    public String technician() {
        return technician;
    }

    public boolean showPerformedServices() {
        return showPerformedServices;
    }

    public boolean showUsedParts() {
        return showUsedParts;
    }

}
