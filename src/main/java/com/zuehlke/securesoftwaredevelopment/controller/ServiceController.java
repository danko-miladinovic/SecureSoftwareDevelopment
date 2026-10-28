package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.domain.CreateServiceRequest;
import com.zuehlke.securesoftwaredevelopment.repository.ServiceRepository;
import com.zuehlke.securesoftwaredevelopment.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class ServiceController {

    private static final int MAX_TEXT_LENGTH = 100;
    private static final int MAX_PERFORMED_SERVICES = 20;

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public ServiceController(ServiceRepository serviceRepository, UserRepository userRepository) {
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/api/my/services", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> create(@RequestBody CreateServiceRequest request) {
        validate(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int authenticatedCustomerId = userRepository.findIdByUsername(authentication.getName());
        int serviceId = serviceRepository.create(authenticatedCustomerId, request);
        return Collections.singletonMap("serviceId", serviceId);
    }

    private void validate(CreateServiceRequest request) {
        requireText(request.getCarModel(), "Car model");
        requireText(request.getTechnician(), "Technician");
        Double totalPrice = request.getTotalPrice();
        if (totalPrice == null || totalPrice.isNaN() || totalPrice.isInfinite() || totalPrice < 0) {
            throw badRequest("Total price must be a non-negative number.");
        }
        List<CreateServiceRequest.PerformedService> performedServices = request.getPerformedServices();
        if (performedServices == null || performedServices.isEmpty()
                || performedServices.size() > MAX_PERFORMED_SERVICES) {
            throw badRequest("Add between 1 and " + MAX_PERFORMED_SERVICES + " performed services.");
        }
        for (CreateServiceRequest.PerformedService performedService : performedServices) {
            if (performedService == null) {
                throw badRequest("Performed service is required.");
            }
            requireText(performedService.getName(), "Performed service");
            if (performedService.getUsedParts() != null) {
                for (String part : performedService.getUsedParts()) {
                    requireText(part, "Used part");
                }
            }
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > MAX_TEXT_LENGTH) {
            throw badRequest(field + " is required and must be at most " + MAX_TEXT_LENGTH + " characters.");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
