package launchpath.userservice.co.dto.response;

import launchpath.userservice.co.enums.PlanName;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PlanResponseDTO {

    private Integer id;
    private PlanName name;
    private BigDecimal priceInr;
    private Integer atsCredits;
    private Integer resumeDownloads;
    private Integer maxResumes;
    private Integer templatesCount;
}