package com.marlow.seed;

import com.marlow.intelligence.AnalysisInput;
import com.marlow.intelligence.ReviewInsight;
import com.marlow.intelligence.ReviewIntelligence;
import com.marlow.review.ReplyStatus;
import com.marlow.review.Review;
import com.marlow.review.ReviewMapper;
import com.marlow.review.ReviewRepository;
import com.marlow.tenant.ShopRole;
import com.marlow.tenant.Tenant;
import com.marlow.tenant.TenantRepository;
import com.marlow.tenant.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@Profile("!test")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String ELM_KEY = "marlow_demo_elm_oak_7f3a";
    public static final String BRIGHT_KEY = "marlow_demo_brightline_2c91";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final TenantRepository tenants;
    private final TenantService tenantService;
    private final ReviewRepository reviews;
    private final ReviewIntelligence intelligence;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ZoneId zone;

    public DemoDataSeeder(TenantRepository tenants, TenantService tenantService, ReviewRepository reviews,
                          ReviewIntelligence intelligence, ObjectMapper objectMapper, Clock clock, ZoneId zone) {
        this.tenants = tenants;
        this.tenantService = tenantService;
        this.reviews = reviews;
        this.intelligence = intelligence;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.zone = zone;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tenants.countBySlug("elm-oak") > 0) {
            return;
        }
        Tenant elm = tenantService.createShop("Elm & Oak Bakery", "elm-oak", "bakery", "Chandigarh");
        tenantService.addUser(elm.getId(), "harpreet@elmandoak.example", "Harpreet Kaur", ShopRole.OWNER, "desk-local-only");
        tenantService.addUser(elm.getId(), "aman@elmandoak.example", "Aman Gill", ShopRole.STAFF, "desk-local-only");
        tenantService.issueKey(elm.getId(), "Counter iPad", ELM_KEY);
        seed(elm, bakery());

        Tenant bright = tenantService.createShop("Brightline Dental", "brightline", "dental clinic", "Patiala");
        tenantService.addUser(bright.getId(), "neha@brightline.example", "Dr. Neha Sethi", ShopRole.OWNER, "desk-local-only");
        tenantService.addUser(bright.getId(), "rhea@brightline.example", "Rhea Malhotra", ShopRole.STAFF, "desk-local-only");
        tenantService.issueKey(bright.getId(), "Front desk", BRIGHT_KEY);
        seed(bright, dental());

        log.info("Demo desks are ready. Elm & Oak key: {}  Brightline key: {}", ELM_KEY, BRIGHT_KEY);
    }

    private void seed(Tenant tenant, List<SeedReview> rows) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        for (SeedReview row : rows) {
            Review review = new Review();
            review.setTenantId(tenant.getId());
            review.setAuthorName(row.author());
            review.setRating(row.rating());
            review.setSource(row.source());
            review.setBody(row.text());
            review.setReviewedAt(today.minusDays(row.daysAgo()).atTime(row.hour(), 15).atZone(zone).toInstant());
            review.setReplyStatus(row.sent() ? ReplyStatus.SENT : ReplyStatus.DRAFT);
            ReviewInsight insight = intelligence.analyze(new AnalysisInput(
                    tenant.getName(), tenant.getIndustry(), row.author(), row.rating(), row.source(), row.text(), "warm"));
            review.setSentiment(insight.sentiment());
            review.setSentimentScore(insight.score());
            review.setCategory(insight.category());
            review.setThemes(ReviewMapper.writeThemes(insight.themes(), objectMapper));
            review.setDraftReply(insight.draftReply());
            review.setModelName(insight.model());
            reviews.save(review);
        }
    }

    private static List<SeedReview> bakery() {
        return List.of(
                new SeedReview(2, 11, "Ananya Sharma", 5, "Google", false,
                        "The almond croissant was fresh and flaky, and the cardamom bun was perfect. I would recommend Elm & Oak to anyone in Sector 17."),
                new SeedReview(3, 9, "Rohit Verma", 2, "Zomato", false,
                        "The sourdough arrived late and the box was crushed. Delivery to Sector 8 was awful."),
                new SeedReview(5, 16, "Simran Kaur", 5, "Instagram", true,
                        "Aman at the counter was kind and the coffee was delicious. Cozy seating and lovely music."),
                new SeedReview(6, 8, "Kabir Mehta", 1, "Google", false,
                        "The croissant was stale and the coffee tasted burnt. Terrible morning."),
                new SeedReview(8, 13, "Meher Joshi", 4, "Walk-in card", true,
                        "Beautiful cake and the staff were polite. A warm room for a rainy day."),
                new SeedReview(9, 18, "Isha Narang", 2, "Google", false,
                        "Overpriced for a dry bun. The bill felt expensive for what we got."),
                new SeedReview(12, 10, "Arjun Bedi", 5, "Google", true,
                        "Best sourdough in Chandigarh. Fresh, tasty, and the team was helpful."),
                new SeedReview(14, 15, "Neel Kapoor", 3, "Zomato", false,
                        "The soup was fine. Nothing special about the taste, and the seating was crowded."),
                new SeedReview(16, 12, "Tara Singh", 1, "Instagram", false,
                        "Rude reply when I called about a wrong cake order. The sponge was dry and I felt ignored."),
                new SeedReview(18, 17, "Vikram Ahuja", 5, "Google", true,
                        "Outstanding cookies and a prompt team. I love the Saturday bun."),
                new SeedReview(21, 9, "Pooja Seth", 2, "Zomato", false,
                        "Delivery was delayed and the pastry was cold and soggy. Disappointed."),
                new SeedReview(24, 14, "Dev Malhotra", 4, "Walk-in card", true,
                        "Friendly barista and a great chai. The interior feels cozy."),
                new SeedReview(28, 11, "Sana Qureshi", 5, "Google", true,
                        "Perfect birthday cake. Beautiful icing and the taste was wonderful."),
                new SeedReview(33, 19, "Gurleen Brar", 2, "Google", false,
                        "Dirty table and a slow counter. I waited and the bun was bland."),
                new SeedReview(37, 8, "Harman Singh", 4, "Instagram", true,
                        "Fresh bread, fair price, and the staff were professional."),
                new SeedReview(41, 16, "Aisha Khan", 1, "Zomato", false,
                        "The package arrived crushed. Worst delivery, and the cake was broken.")
        );
    }

    private static List<SeedReview> dental() {
        return List.of(
                new SeedReview(1, 10, "Rahul Khanna", 5, "Practo", true,
                        "Dr. Sethi was gentle and the cleaning was painless. I would recommend Brightline."),
                new SeedReview(4, 15, "Nikita Rao", 1, "Google", false,
                        "I waited past my appointment and the receptionist was rude. Horrible visit."),
                new SeedReview(6, 12, "Manav Gill", 4, "Practo", true,
                        "The crown feels smooth and the price was fair. Professional and careful."),
                new SeedReview(7, 18, "Aditi Shah", 2, "Google", false,
                        "They charged me twice and ignored my calls. The bill was expensive and wrong."),
                new SeedReview(10, 9, "Jaspreet Kaur", 5, "Walk-in card", true,
                        "Painless filling and a kind doctor. The clinic is clean and calm."),
                new SeedReview(13, 14, "Omar Farooq", 3, "Practo", false,
                        "The appointment was on time. Treatment was fine, nothing outstanding."),
                new SeedReview(15, 11, "Lavanya Iyer", 1, "Google", false,
                        "The filling still hurts and the room did not feel clean. Disappointed and painful."),
                new SeedReview(19, 16, "Kunal Dey", 5, "Google", true,
                        "Best dental visit I have had. Gentle, prompt, and the staff were helpful."),
                new SeedReview(22, 8, "Ritika Bose", 2, "Practo", false,
                        "Overpriced aligner consult. I felt rushed and the explanation was careless."),
                new SeedReview(25, 13, "Sameer Ahluwalia", 4, "Google", true,
                        "Clean clinic, polite nurse, and the tooth feels great."),
                new SeedReview(29, 17, "Pallavi Nair", 2, "Google", false,
                        "Called the helpline and nobody replied. My appointment was missed."),
                new SeedReview(31, 10, "Yash Mittal", 5, "Practo", true,
                        "Wonderful with a nervous patient. The doctor was kind and the cleaning was excellent."),
                new SeedReview(36, 12, "Noor Sheikh", 3, "Walk-in card", false,
                        "The crown is okay. Parking was a crowd and the wait was ordinary."),
                new SeedReview(40, 9, "Ishaan Malhotra", 1, "Google", false,
                        "Terrible pain after the treatment and the front desk was rude when I called."),
                new SeedReview(44, 15, "Diya Chopra", 5, "Google", true,
                        "I love how careful they were with the crown. Professional team and a fair bill."),
                new SeedReview(48, 11, "Arnav Bhatia", 2, "Practo", false,
                        "Delayed appointment and a cold room. The service felt unprofessional.")
        );
    }

    private record SeedReview(int daysAgo, int hour, String author, int rating, String source, boolean sent, String text) {
    }
}
