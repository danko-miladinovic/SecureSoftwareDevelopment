package com.zuehlke.securesoftwaredevelopment;

import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecureSoftwareDevelopmentApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MockHttpSession session;

    @BeforeEach
    void login() throws Exception {
        session = (MockHttpSession) mvc.perform(post("/perform-login")
                        .param("username", "bruce").param("password", "wayne"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cars"))
                .andReturn().getRequest().getSession(false);
    }

    @AfterEach
    void removeCreatedServices() {
        // Seeded services end at 301; anything above was created by a test.
        mongoTemplate.getCollection("services").deleteMany(new Document("serviceId", new Document("$gt", 301)));
    }

    @Test
    void defaultSearchReturnsOnlyOwnCompletedServicesInDescendingOrder() throws Exception {
        search("{}")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].serviceId").value(102))
                .andExpect(jsonPath("$[1].serviceId").value(101))
                .andExpect(jsonPath("$[0]._id").doesNotExist())
                .andExpect(jsonPath("$[0].customerId").doesNotExist())
                .andExpect(jsonPath("$[0].pricingPolicy").doesNotExist())
                .andExpect(jsonPath("$[0].performedServices").doesNotExist());
    }

    @Test
    void normalRequestFromPaperIncludesServicesAndParts() throws Exception {
        search("{\"filters\":{\"carModel\":\"Ford Focus\"},\"view\":{"
                + "\"performedServices.name\":1,\"performedServices.usedParts\":1}}")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].performedServices[0].name").value("Oil change"))
                .andExpect(jsonPath("$[1].performedServices[0].usedParts[0].name").value("Oil filter"));
    }

    @Test
    void allFourDomainFiltersWorkTogether() throws Exception {
        search("{\"filters\":{\"carModel\":\"Ford Focus\","
                + "\"performedServices.name\":\"Oil change\","
                + "\"performedServices.usedParts.name\":\"Oil filter\",\"technician\":\"Marko\"}}")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].serviceId").value(101));
        search("{\"filters\":{\"performedServices.usedParts.name\":\"Unknown part\"}}")
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void combinedAttackFromPaperReadsOtherCustomersAndInternalFields() throws Exception {
        // This behavior is intentional for the CWE-943 demonstration.
        search("{\"filters\":{\"customerId\":{\"$ne\":1}},"
                + "\"view\":{\"customerId\":1,\"pricingPolicy\":1}}")
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].serviceId").value(202))
                .andExpect(jsonPath("$[0].customerId").value(2))
                .andExpect(jsonPath("$[0].pricingPolicy").value("LOYALTY"))
                .andExpect(jsonPath("$[1].serviceId").value(201))
                .andExpect(jsonPath("$[2].serviceId").value(301))
                .andExpect(jsonPath("$[2].customerId").value(3))
                .andExpect(jsonPath("$[2].pricingPolicy").value("FLEET"));
    }

    @Test
    void clientCannotOverrideCompletedServiceRestriction() throws Exception {
        search("{\"filters\":{\"completedAt\":null}}")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].serviceId").value(102))
                .andExpect(jsonPath("$[1].serviceId").value(101));
    }

    @Test
    void anotherLoginUsesItsOwnCustomerId() throws Exception {
        MockHttpSession peter = (MockHttpSession) mvc.perform(post("/perform-login")
                        .param("username", "peter").param("password", "security_rules"))
                .andExpect(redirectedUrl("/cars"))
                .andReturn().getRequest().getSession(false);
        mvc.perform(post("/api/my/service-history/search").session(peter)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].serviceId").value(202))
                .andExpect(jsonPath("$[1].serviceId").value(201));
    }

    @Test
    void pageRendersForLoggedInCustomer() throws Exception {
        mvc.perform(get("/my/service-history").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Completed service history")))
                .andExpect(content().string(containsString("/api/my/service-history/search")));
    }

    @Test
    void anonymousRequestsRequireLogin() throws Exception {
        mvc.perform(post("/api/my/service-history/search")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(get("/my/service-history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void createdServiceBelongsToLoggedInCustomerAndShowsInHistory() throws Exception {
        int serviceId = createService("{\"carModel\":\" Ford Focus \",\"technician\":\"Ana\",\"totalPrice\":150.5,"
                + "\"performedServices\":[{\"name\":\"Tire rotation\",\"usedParts\":[\"Wheel weights\"]}]}");

        assertEquals(302, serviceId);
        Document stored = storedService(serviceId);
        assertEquals(1, stored.getInteger("customerId"));
        assertEquals("COMPLETED", stored.getString("status"));
        assertEquals("RETAIL", stored.getString("pricingPolicy"));
        assertEquals("Ford Focus", stored.getString("carModel"));

        search("{}")
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].serviceId").value(serviceId))
                .andExpect(jsonPath("$[0].carModel").value("Ford Focus"))
                .andExpect(jsonPath("$[0].pricingPolicy").doesNotExist());
    }

    @Test
    void clientCannotChooseOwnerStatusOrPricingPolicy() throws Exception {
        int serviceId = createService("{\"carModel\":\"Ford Focus\",\"technician\":\"Ana\",\"totalPrice\":10,"
                + "\"performedServices\":[{\"name\":\"Wash\"}],"
                + "\"serviceId\":1,\"customerId\":2,\"status\":\"SCHEDULED\",\"pricingPolicy\":\"FLEET\"}");

        Document stored = storedService(serviceId);
        assertEquals(1, stored.getInteger("customerId"));
        assertEquals("COMPLETED", stored.getString("status"));
        assertEquals("RETAIL", stored.getString("pricingPolicy"));
    }

    @Test
    void operatorsAndInvalidServicesAreRejected() throws Exception {
        String[] invalidBodies = {
                "{\"carModel\":{\"$ne\":null},\"technician\":\"Ana\",\"totalPrice\":10,"
                        + "\"performedServices\":[{\"name\":\"Wash\"}]}",
                "{\"carModel\":\"Ford Focus\",\"technician\":\"Ana\",\"totalPrice\":10,"
                        + "\"performedServices\":[{\"name\":{\"$gt\":\"\"}}]}",
                "{\"technician\":\"Ana\",\"totalPrice\":10,\"performedServices\":[{\"name\":\"Wash\"}]}",
                "{\"carModel\":\"Ford Focus\",\"technician\":\"Ana\",\"totalPrice\":-1,"
                        + "\"performedServices\":[{\"name\":\"Wash\"}]}",
                "{\"carModel\":\"Ford Focus\",\"technician\":\"Ana\",\"totalPrice\":10,\"performedServices\":[]}",
                "{\"carModel\":\"Ford Focus\",\"technician\":\"Ana\",\"totalPrice\":10,"
                        + "\"performedServices\":[{\"name\":\"Wash\",\"usedParts\":[\" \"]}]}"
        };
        for (String body : invalidBodies) {
            mvc.perform(post("/api/my/services").session(session)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, mongoTemplate.getCollection("services")
                .countDocuments(new Document("serviceId", new Document("$gt", 301))));
    }

    @Test
    void createServicePageRendersForLoggedInCustomerOnly() throws Exception {
        mvc.perform(get("/my/services/new").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/my/services")));
        mvc.perform(get("/my/services/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(post("/api/my/services")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    private int createService(String body) throws Exception {
        String response = mvc.perform(post("/api/my/services").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.serviceId");
    }

    private Document storedService(int serviceId) {
        return mongoTemplate.getCollection("services").find(new Document("serviceId", serviceId)).first();
    }

    private ResultActions search(String body) throws Exception {
        return mvc.perform(post("/api/my/service-history/search").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

}
