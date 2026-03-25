package in.skillsync.skill.service;

import in.skillsync.common.exception.DuplicateSkillException;
import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.skill.dto.SkillRequest;
import in.skillsync.skill.dto.SkillResponse;
import in.skillsync.skill.entity.Skill;
import in.skillsync.skill.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillService {

    private final SkillRepository skillRepository;

    @CacheEvict(value = "skills", allEntries = true)
    @Transactional
    public SkillResponse createSkill(SkillRequest request) {
        if (skillRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateSkillException(
                    "Skill already exists: " + request.getName());
        }

        Skill skill = Skill.builder()
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .build();

        Skill saved = skillRepository.save(skill);
        log.info("Skill created: {}", saved.getName());
        return mapToResponse(saved);
    }

    @Cacheable(value = "skills", key = "'all'")
    public List<SkillResponse> getAllSkills() {
        log.debug("Fetching all skills");
        return skillRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "skills", key = "#id")
    public SkillResponse getSkillById(Long id) {
        return skillRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));
    }

    public List<SkillResponse> getSkillsByCategory(String category) {
        return skillRepository.findByCategoryIgnoreCase(category)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "skills", allEntries = true)
    @Transactional
    public SkillResponse updateSkill(Long id, SkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));

        skill.setName(request.getName());
        skill.setCategory(request.getCategory());
        skill.setDescription(request.getDescription());

        Skill updated = skillRepository.save(skill);
        log.info("Skill updated: id={}", id);
        return mapToResponse(updated);
    }

    @CacheEvict(value = "skills", allEntries = true)
    @Transactional
    public void deleteSkill(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Skill", "id", id);
        }
        skillRepository.deleteById(id);
        log.info("Skill deleted: id={}", id);
    }

    private SkillResponse mapToResponse(Skill skill) {
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .category(skill.getCategory())
                .description(skill.getDescription())
                .build();
    }
}
