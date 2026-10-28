package com.zuehlke.securesoftwaredevelopment.domain;

import java.util.List;

public class CreateServiceRequest {
    private String carModel;
    private String technician;
    private Double totalPrice;
    private List<PerformedService> performedServices;

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getTechnician() {
        return technician;
    }

    public void setTechnician(String technician) {
        this.technician = technician;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<PerformedService> getPerformedServices() {
        return performedServices;
    }

    public void setPerformedServices(List<PerformedService> performedServices) {
        this.performedServices = performedServices;
    }

    public static class PerformedService {
        private String name;
        private List<String> usedParts;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getUsedParts() {
            return usedParts;
        }

        public void setUsedParts(List<String> usedParts) {
            this.usedParts = usedParts;
        }
    }
}
