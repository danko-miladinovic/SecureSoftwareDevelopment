package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.domain.ServiceHistorySearchRequest;
import com.zuehlke.securesoftwaredevelopment.repository.ServiceHistoryRepository;
import com.zuehlke.securesoftwaredevelopment.repository.UserRepository;
import org.bson.Document;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ServiceHistoryController {

    private final ServiceHistoryRepository serviceHistoryRepository;
    private final UserRepository userRepository;

    public ServiceHistoryController(ServiceHistoryRepository serviceHistoryRepository, UserRepository userRepository) {
        this.serviceHistoryRepository = serviceHistoryRepository;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/api/my/service-history/search", produces = "application/json")
    public List<Document> search(@RequestBody ServiceHistorySearchRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int authenticatedCustomerId = userRepository.findIdByUsername(authentication.getName());
        return serviceHistoryRepository.search(authenticatedCustomerId, request.getFilters(), request.getView());
    }
}
