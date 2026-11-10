package com.ga.gymio.controller;

import com.ga.gymio.dto.request.ProfileRequest;
import com.ga.gymio.dto.response.ProfileResponse;
import com.ga.gymio.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Provides REST endpoints for creating, retrieving, and updating user profiles.
 *
 * Supports profile data and image uploads using multipart form data.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * Creates a profile for the current user.
     *
     * Accepts validated profile details and an image file.
     *
     * @param request the validated profile details
     * @param image the image file to associate with the profile
     * @return the created profile details
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse createProfile(
            @Valid @RequestPart("profile") ProfileRequest request,
            @RequestPart("image") MultipartFile image) {

        return userProfileService.createProfile(request, image);
    }

    /**
     * Retrieves the profile of the current user.
     *
     * @return the current user's profile details
     */
    @GetMapping
    public ProfileResponse getProfile() {

        return userProfileService.getProfile();
    }

    /**
     * Updates the profile of the current user.
     *
     * Accepts validated profile details and an optional replacement image.
     * If no image is provided, the service determines how the existing
     * profile image is handled.
     *
     * @param request the validated profile details
     * @param image the optional replacement image file
     * @return the updated profile details
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse updateProfile(
            @Valid @RequestPart("profile") ProfileRequest request,
            @RequestPart(value = "image", required = false)
                    MultipartFile image) {

        return userProfileService.updateProfile(request, image);
    }
}