package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.FittingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualFittingStatusResponse {

    private Long taskId;
    private FittingStatus status;
    private String resultImgUrl;
}

