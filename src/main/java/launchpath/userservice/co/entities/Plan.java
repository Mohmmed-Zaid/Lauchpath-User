package launchpath.userservice.co.entities;

import launchpath.userservice.co.entities.base.BaseEntity;
import launchpath.userservice.co.enums.PlanName;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Plan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private PlanName name; // FREE, STARTER, PRIME, AGENCY

    @Column(name = "price_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceInr; // BigDecimal for money — NEVER float/double (precision loss)

    // -1 = unlimited. Cleaner than null for "no limit" checks in service layer
    @Column(name = "ats_credits", nullable = false)
    private Integer atsCredits;

    @Column(name = "resume_downloads", nullable = false)
    private Integer resumeDownloads;

    @Column(name = "max_resumes", nullable = false)
    private Integer maxResumes;

    @Column(name = "templates_count", nullable = false)
    private Integer templatesCount;
}
