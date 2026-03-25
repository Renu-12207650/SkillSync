package in.skillsync.skill.controller;

import in.skillsync.skill.dto.SkillRequest;
import in.skillsync.skill.dto.SkillResponse;
import in.skillsync.skill.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for skill catalogue management.
 * Base path: /skills
 */
@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
@Tag(name = "Skills", description = "Skill catalogue management")
@SecurityRequirement(name = "bearerAuth")
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create a new skill (ROLE_ADMIN only)")
    public ResponseEntity<SkillResponse> createSkill(
            @Valid @RequestBody SkillRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(skillService.createSkill(request));
    }

    @GetMapping
    @Operation(summary = "Get all skills (cached)")
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get skill by ID")
    public ResponseEntity<SkillResponse> getSkillById(@PathVariable Long id) {
        return ResponseEntity.ok(skillService.getSkillById(id));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get skills by category")
    public ResponseEntity<List<SkillResponse>> getSkillsByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(skillService.getSkillsByCategory(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update a skill (ROLE_ADMIN only)")
    public ResponseEntity<SkillResponse> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody SkillRequest request) {
        return ResponseEntity.ok(skillService.updateSkill(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Delete a skill (ROLE_ADMIN only)")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }
}
