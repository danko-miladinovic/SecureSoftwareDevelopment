package com.zuehlke.securesoftwaredevelopment.repository;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ServiceHistoryRepository {

    private final MongoTemplate mongoTemplate;

    public ServiceHistoryRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Document> search(int authenticatedCustomerId, Map<String, Object> filters, Map<String, Object> view) {
        Map<String, Object> effectiveFilters = filters != null ? filters : new HashMap<>();
        Map<String, Object> effectiveView = view != null ? view : new HashMap<>();

        Document match = new Document(effectiveFilters);
        match.putIfAbsent("customerId", authenticatedCustomerId);
        match.put("completedAt", new Document("$ne", null));

        Document projection = baseProjection();
        projection.putAll(effectiveView);

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

    private Document baseProjection() {
        return new Document("_id", 0)
                .append("serviceId", 1)
                .append("carModel", 1)
                .append("completedAt", 1)
                .append("totalPrice", 1);
    }
}
