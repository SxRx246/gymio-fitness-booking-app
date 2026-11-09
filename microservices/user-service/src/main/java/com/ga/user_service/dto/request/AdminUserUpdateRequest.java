package com.ga.user_service.dto.request;

import com.ga.user_service.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserUpdateRequest {

    private User.Role role;

    private User.Status status;
}

