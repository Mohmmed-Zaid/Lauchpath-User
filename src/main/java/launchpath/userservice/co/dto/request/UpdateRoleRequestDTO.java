package launchpath.userservice.co.dto.request;

import jakarta.validation.constraints.NotNull;
import launchpath.userservice.co.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequestDTO {

    @NotNull(message = "Role is required")
    private UserRole role;
}