package org.example.controller;

import org.example.model.Requirement;
import org.example.service.FirebaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requirements")
public class RequirementController {

    @Autowired
    private FirebaseService firebaseService;

    @GetMapping
    public ResponseEntity<List<Requirement>> getAllRequirements() {
        try {
            return ResponseEntity.ok(firebaseService.getAllRequirements());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveRequirement(@PathVariable String id) {
        try {
            firebaseService.updateRequirementStatus(id, Requirement.Status.APPROVED);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequirement(@PathVariable String id) {
        try {
            firebaseService.updateRequirementStatus(id, Requirement.Status.REJECTED);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
