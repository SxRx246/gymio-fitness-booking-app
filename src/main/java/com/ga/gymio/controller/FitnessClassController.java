package com.ga.gymio.controller;

import com.ga.gymio.dto.request.FitnessClassRequest;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.service.FitnessClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fitness-classes")
public class FitnessClassController {
    private final FitnessClassService fitnessClassService;

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FitnessClass createFitnessClass(@Valid @RequestBody FitnessClassRequest fitnessClassRequest){
        return fitnessClassService.createFitnessClass(fitnessClassRequest);
    }

    @GetMapping("/{id}")
    public FitnessClass getFitnessClass(@PathVariable Long id){
       return fitnessClassService.getFitnessClass(id);

    }

    @GetMapping("/trainer/{trainerId}")
    public List<FitnessClass> getFitnessClasses(@PathVariable Long trainerId){
        return fitnessClassService.getFitnessClasses(trainerId);
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PutMapping("/{id}")
    public FitnessClass updateFitnessClass(@Valid @RequestBody FitnessClassRequest fitnessClassRequest, @PathVariable Long id){
        return fitnessClassService.updateFitnessClass(fitnessClassRequest, id);
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PutMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelFitnessClass(@PathVariable Long id){
        fitnessClassService.cancelFitnessClass(id);
    }
}
