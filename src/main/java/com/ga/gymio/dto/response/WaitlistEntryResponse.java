
package com.ga.gymio.dto.response;

import com.ga.gymio.model.WaitlistEntry.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class WaitlistEntryResponse {

    private Long id;
    private Long fitnessClassId;
    private Status status;
    private LocalDateTime joinedAt;
    private LocalDateTime notifiedAt;
    private LocalDateTime offerExpiresAt;
}
