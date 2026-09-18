package com.workspace.booking.mapper;

import com.workspace.booking.dto.UserRequest;
import com.workspace.booking.dto.UserResponse;
import com.workspace.booking.entity.identity.User;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public User toEntity(UserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        return user;
    }

    public UserResponse toResponse(User user) {
        String base64Image = null;

        // Convert the binary BLOB to a Base64 Data URI
        if (user.getAvatarBlob() != null && user.getAvatarBlob().length > 0) {
            String encoded = Base64.getEncoder().encodeToString(user.getAvatarBlob());
            base64Image = "data:" + user.getAvatarMimeType() + ";base64," + encoded;
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName()) // Combine names
                .email(user.getEmail())
                .phone(user.getPhone())
                .companyName(user.getCompanyName())
                .jobTitle(user.getJobTitle())
                .avatarBase64(base64Image) 
                .roles(user.getRoles() != null ?
                        user.getRoles().stream()
                        .map(ur -> ur.getRole().getCode())
                        .collect(Collectors.toList()) : null)
                .build();
    }
}
