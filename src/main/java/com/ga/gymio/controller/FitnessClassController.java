package com.ga.gymio.controller;

import com.ga.gymio.dto.request.FitnessClassRequest;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.service.FitnessClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fitness-classes")
public class FitnessClassController {
    private final FitnessClassService fitnessClassService;

    @PostMapping
    public ResponseEntity<FitnessClass>  createFitnessClass(@Valid @RequestBody FitnessClassRequest fitnessClassRequest){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(fitnessClassService.createFitnessClass(fitnessClassRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FitnessClass> getFitnessClass(@PathVariable Long id){
       return ResponseEntity.ok(fitnessClassService.getFitnessClass(id));

    }

    @GetMapping("/trainer/{trainerId}")
    public List<FitnessClass> getFitnessClasses(@PathVariable Long trainer_id){
        return fitnessClassService.getFitnessClasses(trainer_id);
    }
}
