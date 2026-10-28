package com.zuehlke.securesoftwaredevelopment.repository;

import com.mongodb.client.MongoCollection;
import com.zuehlke.securesoftwaredevelopment.domain.CreateServiceRequest;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ServiceRepository {

    private static final String DEFAULT_PRICING_POLICY = "RETAIL";

    private final MongoTemplate mongoTemplate;

    public ServiceRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public int create(int authenticatedCustomerId, CreateServiceRequest request) {
        MongoCollection<Document> services = mongoTemplate.getCollection("services");
        int serviceId = nextServiceId(services);

        // Every field name is fixed here and every value is a plain string or number,
        // so the client cannot choose the owner, status or pricing policy.
        Document service = new Document("serviceId", serviceId)
                .append("customerId", authenticatedCustomerId)
                .append("carModel", request.getCarModel().trim())
                .append("status", "COMPLETED")
                .append("completedAt", LocalDate.now().toString())
                .append("totalPrice", request.getTotalPrice())
                .append("pricingPolicy", pricingPolicyOf(services, authenticatedCustomerId))
                .append("technician", request.getTechnician().trim())
                .append("performedServices", request.getPerformedServices().stream()
                        .map(this::performed)
                        .collect(Collectors.toList()));
        services.insertOne(service);
        return serviceId;
    }

    private Document performed(CreateServiceRequest.PerformedService performedService) {
        List<String> usedParts = performedService.getUsedParts() != null
                ? performedService.getUsedParts()
                : Collections.emptyList();
        return new Document("name", performedService.getName().trim())
                .append("usedParts", usedParts.stream()
                        .map(part -> new Document("name", part.trim()))
                        .collect(Collectors.toList()));
    }

    private int nextServiceId(MongoCollection<Document> services) {
        Document last = services.find().sort(new Document("serviceId", -1)).limit(1).first();
        return last != null ? last.getInteger("serviceId") + 1 : 1;
    }

    private String pricingPolicyOf(MongoCollection<Document> services, int customerId) {
        Document existing = services.find(new Document("customerId", customerId)
                .append("pricingPolicy", new Document("$exists", true))).first();
        return existing != null ? existing.getString("pricingPolicy") : DEFAULT_PRICING_POLICY;
    }
}
