package com.ga.gymio.controller;

import com.ga.gymio.dto.request.FitnessClassRequest;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.service.FitnessClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public Page<FitnessClass> getFitnessClasses(
            @PathVariable Long trainerId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return fitnessClassService.getFitnessClasses(trainerId, pageable);
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

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFitnessClass(@PathVariable Long id) {
        fitnessClassService.deleteFitnessClass(id);
    }
}
