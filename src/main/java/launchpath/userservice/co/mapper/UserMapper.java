package launchpath.userservice.co.mapper;

import launchpath.userservice.co.dto.response.*;
import launchpath.userservice.co.entities.FileRecord;
import launchpath.userservice.co.entities.Plan;
import launchpath.userservice.co.entities.Subscription;
import launchpath.userservice.co.entities.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDTO toUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .subscription(
                        user.getSubscription() != null
                                ? toSubscriptionResponseDTO(user.getSubscription())
                                : null
                )
                .build();
    }

    public SubscriptionResponseDTO toSubscriptionResponseDTO(Subscription sub) {
        int totalAts = sub.getPlan().getAtsCredits();
        int totalDownloads = sub.getPlan().getResumeDownloads();

        return SubscriptionResponseDTO.builder()
                .id(sub.getId())
                .planName(sub.getPlan().getName())
                .status(sub.getStatus())
                .totalAtsCredits(totalAts)
                .atsCreditsUsed(sub.getAtsCreditsUsed())
                .remainingAtsCredits(
                        totalAts == -1
                                ? Integer.MAX_VALUE
                                : totalAts - sub.getAtsCreditsUsed()
                )
                .totalDownloads(totalDownloads)
                .downloadsUsed(sub.getDownloadsUsed())
                .remainingDownloads(
                        totalDownloads == -1
                                ? Integer.MAX_VALUE
                                : totalDownloads - sub.getDownloadsUsed()
                )
                .maxResumes(sub.getPlan().getMaxResumes())
                .templatesCount(sub.getPlan().getTemplatesCount())
                .startsAt(sub.getStartsAt())
                .expiresAt(sub.getExpiresAt())
                .build();
    }

    public PlanResponseDTO toPlanResponseDTO(Plan plan) {
        return PlanResponseDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .priceInr(plan.getPriceInr())
                .atsCredits(plan.getAtsCredits())
                .resumeDownloads(plan.getResumeDownloads())
                .maxResumes(plan.getMaxResumes())
                .templatesCount(plan.getTemplatesCount())
                .build();
    }

    public FileRecordResponseDTO toFileRecordResponseDTO(FileRecord record) {
        return FileRecordResponseDTO.builder()
                .id(record.getId())
                .cloudinaryId(record.getCloudinaryId())
                .url(record.getUrl())
                .fileType(record.getFileType())
                .sizeBytes(record.getSizeBytes())
                .createdAt(record.getCreatedAt())
                .build();
    }
}