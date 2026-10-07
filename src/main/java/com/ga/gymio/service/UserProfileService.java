package com.ga.gymio.service;

import com.ga.gymio.dto.request.ProfileRequest;
import com.ga.gymio.dto.response.ProfileResponse;
import com.ga.gymio.exception.InformationExistsException;
import com.ga.gymio.exception.InformationNotFoundException;
import com.ga.gymio.model.AuditLog;
import com.ga.gymio.model.User;
import com.ga.gymio.model.UserProfile;
import com.ga.gymio.repository.UserProfileRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private final String uploadDir = "profile-images/";

    public ProfileResponse createProfile(
            ProfileRequest request,
            MultipartFile image) {

        User user = getAuthenticatedUser();

        if (userProfileRepository.existsByUserId(user.getId())) {
            throw new InformationExistsException(
                    "Profile already exists"
            );
        }

        String imageUrl = saveImage(image);

        UserProfile profile = new UserProfile();

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setProfileImage(imageUrl);
        profile.setUser(user);

        UserProfile savedProfile =
                userProfileRepository.save(profile);

        auditLogService.log(
                AuditLog.Action.CREATE_PROFILE,
                "User " + user.getId() + " created their profile",
                user
        );

        return mapToResponse(savedProfile);
    }

    public ProfileResponse getProfile() {

        User user = getAuthenticatedUser();

        UserProfile profile =
                userProfileRepository.findByUserId(user.getId())
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Profile not found"
                                )
                        );

        return mapToResponse(profile);
    }

    public ProfileResponse updateProfile(
            ProfileRequest request,
            MultipartFile image) {

        User user = getAuthenticatedUser();

        UserProfile profile =
                userProfileRepository.findByUserId(user.getId())
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Profile not found"
                                )
                        );

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setDateOfBirth(request.getDateOfBirth());

        if (image != null && !image.isEmpty()) {
            String imageUrl = saveImage(image);
            profile.setProfileImage(imageUrl);
        }

        UserProfile updatedProfile =
                userProfileRepository.save(profile);

        auditLogService.log(
                AuditLog.Action.UPDATE_PROFILE,
                "User " + user.getId() + " updated their profile",
                user
        );

        return mapToResponse(updatedProfile);
    }

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "User not found"
                        )
                );
    }

    private ProfileResponse mapToResponse(UserProfile profile) {

        return new ProfileResponse(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhoneNumber(),
                profile.getDateOfBirth(),
                profile.getProfileImage()
        );
    }

    private String saveImage(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "Profile image is required"
            );
        }

        String contentType = image.getContentType();

        if (contentType == null ||
                (!contentType.equals("image/jpeg")
                        && !contentType.equals("image/png")
                        && !contentType.equals("image/webp"))) {

            throw new IllegalArgumentException(
                    "Only JPEG, PNG, and WEBP images are allowed"
            );
        }

        try {

            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename =
                    image.getOriginalFilename();

            String extension = "";

            if (originalFilename != null &&
                    originalFilename.contains(".")) {

                extension = originalFilename.substring(
                        originalFilename.lastIndexOf(".")
                );
            }

            String filename =
                    UUID.randomUUID() + extension;

            Path filePath =
                    uploadPath.resolve(filename);

            Files.copy(
                    image.getInputStream(),
                    filePath
            );

            return uploadDir + filename;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to save profile image"
            );
        }
    }



}
