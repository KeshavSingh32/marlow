package com.marlow;

import com.marlow.tenant.ShopRole;
import com.marlow.tenant.Tenant;
import com.marlow.tenant.TenantService;
import com.marlow.upload.UploadJob;
import com.marlow.upload.UploadJobRepository;
import com.marlow.upload.UploadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeskFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantService tenants;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job reviewCsvImportJob;

    @Autowired
    private UploadJobRepository uploadJobs;

    @Test
    void shopsCannotSeeEachOthersReviewsAndAnalyticsStayInsideTheShop() throws Exception {
        Tenant elm = tenants.createShop("Elm Test", "elm-test", "bakery", "Chandigarh");
        Tenant bright = tenants.createShop("Bright Test", "bright-test", "dental clinic", "Patiala");
        tenants.addUser(elm.getId(), "owner@elm.test", "Harpreet", ShopRole.OWNER, "desk-local-only");
        String elmKey = "elm-test-key";
        String brightKey = "bright-test-key";
        tenants.issueKey(elm.getId(), "desk", elmKey);
        tenants.issueKey(bright.getId(), "desk", brightKey);

        mockMvc.perform(post("/api/reviews")
                        .header("X-Api-Key", elmKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"The croissant was stale and burnt","author":"Kabir","rating":1,"source":"Google"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentiment").value("NEGATIVE"))
                .andExpect(jsonPath("$.category").value("PRODUCT"))
                .andExpect(jsonPath("$.draftReply").isNotEmpty());

        mockMvc.perform(get("/api/reviews").header("X-Api-Key", brightKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(get("/api/analytics/summary").header("X-Api-Key", elmKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.negative").value(1));

        mockMvc.perform(get("/api/analytics/categories").header("X-Api-Key", elmKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("PRODUCT"));

        mockMvc.perform(get("/api/analytics/summary").header("X-Api-Key", brightKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(0));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isUnauthorized());

        String created = mockMvc.perform(post("/api/reviews")
                        .header("X-Api-Key", brightKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Dr Sethi was gentle and the cleaning was painless","author":"Rahul Khanna","rating":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentiment").value("POSITIVE"))
                .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll("(?s).*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/reviews/" + id + "/redraft")
                        .header("X-Api-Key", brightKey)
                        .param("tone", "brief"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replyStatus").value("DRAFT"));

        mockMvc.perform(patch("/api/reviews/" + id)
                        .header("X-Api-Key", brightKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"draftReply":"Hi Rahul, glad the cleaning was easy.","replyStatus":"SENT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replyStatus").value("SENT"))
                .andExpect(jsonPath("$.draftReply").value("Hi Rahul, glad the cleaning was easy."));

        mockMvc.perform(get("/api/tenant").header("X-Api-Key", elmKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("elm-test"))
                .andExpect(jsonPath("$.members[0].name").value("Harpreet"))
                .andExpect(jsonPath("$.keys[0].prefix").value("elm-test-key"));
    }

    @Test
    void csvImportRunsThroughSpringBatchWithoutBlockingTheCaller() throws Exception {
        Tenant shop = tenants.createShop("Batch Bakery", "batch-bakery", "bakery", "Chandigarh");
        Path csv = Files.createTempFile("marlow", ".csv");
        Files.writeString(csv, """
                author,rating,source,reviewed_at,text
                Ananya,5,Google,2026-10-01,"The almond croissant was fresh and perfect"
                Rohit,2,Zomato,2026-10-03,"Delivery was late and the box was crushed"
                
                """);

        UUID jobId = UUID.randomUUID();
        UploadJob upload = new UploadJob();
        upload.setId(jobId);
        upload.setTenantId(shop.getId());
        upload.setOriginalFilename("past-reviews.csv");
        upload.setStoredPath(csv.toAbsolutePath().toString());
        upload.setStatus(UploadStatus.QUEUED);
        uploadJobs.save(upload);

        jobLauncher.run(reviewCsvImportJob, new JobParametersBuilder()
                .addString("jobId", jobId.toString())
                .addString("tenantId", shop.getId().toString())
                .addString("filePath", csv.toAbsolutePath().toString())
                .toJobParameters());

        UploadJob finished = uploadJobs.findById(jobId).orElseThrow();
        assertEquals(UploadStatus.COMPLETED, finished.getStatus());
        assertEquals(2, finished.getProcessedRows());

        tenants.issueKey(shop.getId(), "desk", "batch-key");
        mockMvc.perform(get("/api/reviews").header("X-Api-Key", "batch-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }
}
