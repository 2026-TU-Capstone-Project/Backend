package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.FittingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VirtualFittingStatusResponse {

    private Long taskId;
    private FittingStatus status;
    private String resultImgUrl;
}

