package launchpath.userservice.co.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ForgotPasswordRequestDTO {

    @NotBlank @Email
    private String email;
}
