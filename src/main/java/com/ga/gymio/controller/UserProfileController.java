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

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse createProfile(
            @Valid @RequestPart("profile") ProfileRequest request,
            @RequestPart("image") MultipartFile image) {

        return userProfileService.createProfile(request, image);
    }

    @GetMapping
    public ProfileResponse getProfile() {

        return userProfileService.getProfile();
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse updateProfile(
            @Valid @RequestPart("profile") ProfileRequest request,
            @RequestPart(value = "image", required = false)
                    MultipartFile image) {

        return userProfileService.updateProfile(request, image);
    }
}