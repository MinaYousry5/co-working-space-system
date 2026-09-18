package com.workspace.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String companyName;
    private String jobTitle;
    private List<String> roles;
    private String avatarBase64;
}
