package com.zuehlke.securesoftwaredevelopment.repository;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.zuehlke.securesoftwaredevelopment.domain.ServiceHistorySearchRequest;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class ServiceHistoryRepository {

    private final MongoTemplate mongoTemplate;

    public ServiceHistoryRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Document> search(int authenticatedCustomerId, ServiceHistorySearchRequest request) {
        Document match = new Document("customerId", authenticatedCustomerId)
                .append("completedAt", new Document("$ne", null));
        addIfPresent(match, "carModel", request.carModel());
        addIfPresent(match, "performedServices.name", request.serviceName());
        addIfPresent(match, "performedServices.usedParts.name", request.partName());
        addIfPresent(match, "technician", request.technician());

        Document projection = new Document("_id", 0)
                .append("serviceId", 1)
                .append("carModel", 1)
                .append("completedAt", 1)
                .append("totalPrice", 1);
        if (request.showPerformedServices()) {
            projection.append("performedServices.name", 1);
        }
        if (request.showUsedParts()) {
            projection.append("performedServices.usedParts", 1);
        }

        List<Document> pipeline = Arrays.asList(
                new Document("$match", match),
                new Document("$project", projection),
                new Document("$sort", new Document("completedAt", -1))
        );

        MongoCollection<Document> services = mongoTemplate.getCollection("services");
        AggregateIterable<Document> result = services.aggregate(pipeline);

        List<Document> documents = new ArrayList<>();
        for (Document document : result) {
            documents.add(document);
        }
        return documents;
    }

    private void addIfPresent(Document match, String field, String value) {
        if (value != null && !value.trim().isEmpty()) {
            match.append(field, value.trim());
        }
    }
}
