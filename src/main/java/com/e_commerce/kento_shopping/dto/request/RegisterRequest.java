package com.e_commerce.kento_shopping.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Please enter a valid email")
    @Email(message = "Please enter a valid email")
    private String email;

    @NotBlank(message = "Please enter a valid phone number")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Please enter a valid phone number")
    private String phoneNumber;

    @NotBlank(message = "Password must be at least 8 characters and contain a number")
    @Size(min = 8, message = "Password must be at least 8 characters and contain a number")
    @Pattern(regexp = ".*[0-9].*", message = "Password must be at least 8 characters and contain a number")
    private String password;

    @NotBlank(message = "Password does not match !!")
    private String confirmPassword;


}
