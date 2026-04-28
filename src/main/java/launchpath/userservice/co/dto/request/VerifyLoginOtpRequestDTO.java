package launchpath.userservice.co.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerifyLoginOtpRequestDTO {

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 6)
    private String otp;
}
