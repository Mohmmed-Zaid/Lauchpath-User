package launchpath.userservice.co.dto.response;

import launchpath.userservice.co.enums.FileType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FileRecordResponseDTO {

    private Long id;
    private String cloudinaryId;
    private String url;
    private FileType fileType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}