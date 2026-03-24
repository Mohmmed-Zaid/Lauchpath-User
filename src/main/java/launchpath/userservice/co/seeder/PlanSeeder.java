package launchpath.userservice.co.seeder;

import launchpath.userservice.co.entities.Plan;
import launchpath.userservice.co.enums.PlanName;
import launchpath.userservice.co.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlanSeeder implements CommandLineRunner {

    private final PlanRepository planRepository;

    @Override
    public void run(String... args) {
        if (planRepository.count() > 0) return;

        List<Plan> plans = List.of(
                Plan.builder()
                        .name(PlanName.FREE)
                        .priceInr(BigDecimal.ZERO)
                        .atsCredits(3)
                        .resumeDownloads(1)
                        .maxResumes(2)
                        .templatesCount(5)
                        .build(),

                Plan.builder()
                        .name(PlanName.STARTER)
                        .priceInr(new BigDecimal("299.00"))
                        .atsCredits(10)
                        .resumeDownloads(10)
                        .maxResumes(5)
                        .templatesCount(10)
                        .build(),

                Plan.builder()
                        .name(PlanName.PRIME)
                        .priceInr(new BigDecimal("599.00"))
                        .atsCredits(50)
                        .resumeDownloads(50)
                        .maxResumes(15)
                        .templatesCount(40)
                        .build(),

                Plan.builder()
                        .name(PlanName.AGENCY)
                        .priceInr(new BigDecimal("1499.00"))
                        .atsCredits(-1)
                        .resumeDownloads(-1)
                        .maxResumes(-1)
                        .templatesCount(-1)
                        .build()
        );

        planRepository.saveAll(plans);
        System.out.println("Plans seeded successfully");
    }
}