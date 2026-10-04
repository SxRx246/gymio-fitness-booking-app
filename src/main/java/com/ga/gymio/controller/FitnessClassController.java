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
    public ResponseEntity<FitnessClass> createFitnessClass(@Valid @RequestBody FitnessClassRequest fitnessClassRequest){
        System.out.println("Calling createFitnessClass() ==>");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(fitnessClassService.createFitnessClass(fitnessClassRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FitnessClass> getFitnessClass(@PathVariable Long id){
        System.out.println("Calling getFitnessClass() ==>");
       return ResponseEntity.ok(fitnessClassService.getFitnessClass(id));

    }

    @GetMapping("/trainer/{trainerId}")
    public List<FitnessClass> getFitnessClasses(@PathVariable Long trainerId){
        System.out.println("Calling getFitnessClasses() ==>");
        return fitnessClassService.getFitnessClasses(trainerId);
    }

    @PutMapping("/{id}")
    public FitnessClass updateFitnessClass(@Valid @RequestBody FitnessClassRequest fitnessClassRequest, @PathVariable Long id){
        System.out.println("Calling updateFitnessClass() ==>");
        return fitnessClassService.updateFitnessClass(fitnessClassRequest, id);
    }

    @PutMapping("/{id}/cancel")
    public void cancelFitnessClass(@PathVariable Long id){
        System.out.println("Calling cancelFitnessClass() ==>");
        fitnessClassService.cancelFitnessClass(id);
        System.out.println("Fitness Class with id " + id + " cancelled" );
    }
}
