package com.ga.gymio.dto.request;

import com.ga.gymio.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserUpdateRequest {

    private User.Role role;

    private User.Status status;
}

