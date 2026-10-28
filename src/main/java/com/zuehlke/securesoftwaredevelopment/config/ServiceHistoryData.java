package com.zuehlke.securesoftwaredevelopment.config;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ServiceHistoryData implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    public ServiceHistoryData(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        MongoCollection<Document> services = mongoTemplate.getCollection("services");
        services.drop();

        services.insertMany(Arrays.asList(
                completed(101, 1, "Ford Focus", "2023-01-10", 320.0, "RETAIL", "Marko",
                        performed("Oil change", Arrays.asList("Oil filter", "5W-30 oil"))),
                completed(102, 1, "Ford Focus", "2023-05-22", 540.0, "RETAIL", "Jovan",
                        performed("Brake service", Arrays.asList("Brake pads", "Brake fluid"))),
                scheduled(103, 1, "Ford Focus"),

                completed(201, 2, "Honda Civic", "2023-03-14", 880.0, "LOYALTY", "Marko",
                        performed("Timing belt", Arrays.asList("Timing belt kit", "Coolant"))),
                completed(202, 2, "Honda Civic", "2023-09-02", 410.0, "LOYALTY", "Ana",
                        performed("AC service", Arrays.asList("Refrigerant"))),

                completed(301, 3, "VW Golf", "2023-02-18", 1250.0, "FLEET", "Ana",
                        performed("Clutch replacement", Arrays.asList("Clutch kit", "Gearbox oil")))
        ));
    }

    private Document completed(int serviceId, int customerId, String carModel, String completedAt,
                               double totalPrice, String pricingPolicy, String technician,
                               Document... performedServices) {
        return new Document("serviceId", serviceId)
                .append("customerId", customerId)
                .append("carModel", carModel)
                .append("status", "COMPLETED")
                .append("completedAt", completedAt)
                .append("totalPrice", totalPrice)
                .append("pricingPolicy", pricingPolicy)
                .append("technician", technician)
                .append("performedServices", Arrays.asList(performedServices));
    }

    private Document scheduled(int serviceId, int customerId, String carModel) {
        return new Document("serviceId", serviceId)
                .append("customerId", customerId)
                .append("carModel", carModel)
                .append("status", "SCHEDULED")
                .append("completedAt", null);
    }

    private Document performed(String name, List<String> usedParts) {
        return new Document("name", name).append("usedParts", usedParts.stream()
                .map(part -> new Document("name", part))
                .collect(Collectors.toList()));
    }
}
