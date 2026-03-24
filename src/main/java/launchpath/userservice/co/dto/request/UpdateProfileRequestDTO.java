package launchpath.userservice.co.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequestDTO {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    private String avatarUrl;
}