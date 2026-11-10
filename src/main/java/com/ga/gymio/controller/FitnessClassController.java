package com.ga.gymio.controller;

import com.ga.gymio.dto.request.FitnessClassRequest;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.service.FitnessClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Provides REST endpoints for creating, retrieving, searching,
 * updating, cancelling, and deleting fitness classes.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/fitness-classes")
public class FitnessClassController {
    private final FitnessClassService fitnessClassService;

    /**
     * Creates a new fitness class.
     * Only trainers and administrators can access this endpoint.
     *
     * @param fitnessClassRequest the validated details of the fitness class
     * @return the newly created fitness class
     */
    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FitnessClass createFitnessClass(@Valid @RequestBody FitnessClassRequest fitnessClassRequest){
        return fitnessClassService.createFitnessClass(fitnessClassRequest);
    }

    /**
     * Retrieves a fitness class by its ID.
     *
     * @param id the ID of the fitness class
     * @return the requested fitness class
     */
    @GetMapping("/{id}")
    public FitnessClass getFitnessClass(@PathVariable Long id){
       return fitnessClassService.getFitnessClass(id);

    }

    /**
     * Retrieves a trainer's fitness classes in paginated form.
     *
     * @param trainerId the ID of the trainer
     * @param pageable pagination and sorting information; defaults to
     *                 10 classes per page
     * @return a page of the trainer's fitness classes
     */
    @GetMapping("/trainer/{trainerId}")
    public Page<FitnessClass> getFitnessClasses(
            @PathVariable Long trainerId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return fitnessClassService.getFitnessClasses(trainerId, pageable);
    }

    /**
     * Searches fitness classes using optional type, difficulty level,
     * and status filters.
     *
     * @param type the optional fitness class type filter
     * @param level the optional difficulty level filter
     * @param status the optional fitness class status filter
     * @param pageable pagination and sorting information; defaults to
     *                 10 classes per page
     * @return a page of fitness classes matching the supplied filters
     */
    @GetMapping
    public Page<FitnessClass> searchFitnessClasses(
            @RequestParam(required = false) FitnessClass.Type type,
            @RequestParam(required = false) FitnessClass.Level level,
            @RequestParam(required = false) FitnessClass.Status status,
            @ParameterObject
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return fitnessClassService.searchFitnessClasses(
                type,
                level,
                status,
                pageable
        );
    }

    /**
     * Updates an existing fitness class.
     * Only trainers and administrators can access this endpoint.
     *
     * @param fitnessClassRequest the validated updated class details
     * @param id the ID of the fitness class to update
     * @return the updated fitness class
     */
    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PutMapping("/{id}")
    public FitnessClass updateFitnessClass(@Valid @RequestBody FitnessClassRequest fitnessClassRequest, @PathVariable Long id){
        return fitnessClassService.updateFitnessClass(fitnessClassRequest, id);
    }

    /**
     * Cancels an existing fitness class.
     * Only trainers and administrators can access this endpoint.
     *
     * @param id the ID of the fitness class to cancel
     */
    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PutMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelFitnessClass(@PathVariable Long id){
        fitnessClassService.cancelFitnessClass(id);
    }

    /**
     * Deletes a fitness class.
     * Only administrators can access this endpoint.
     *
     * @param id the ID of the fitness class to delete
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFitnessClass(@PathVariable Long id) {
        fitnessClassService.deleteFitnessClass(id);
    }
}
