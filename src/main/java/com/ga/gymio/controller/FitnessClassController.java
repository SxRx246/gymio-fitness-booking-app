package com.ga.gymio.controller;

import com.ga.gymio.dto.request.FitnessClassRequest;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.service.FitnessClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FitnessClassController {
    private final FitnessClassService fitnessClassService;
    @PostMapping
    public ResponseEntity<FitnessClass>  createFitnessClass(@Valid @RequestBody FitnessClassRequest fitnessClassRequest){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(fitnessClassService.createFitnessClass(fitnessClassRequest));
    }
}
