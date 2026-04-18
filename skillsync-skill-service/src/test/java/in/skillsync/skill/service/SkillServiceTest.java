package in.skillsync.skill.service;

import in.skillsync.common.exception.DuplicateSkillException;
import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.skill.dto.SkillRequest;
import in.skillsync.skill.dto.SkillResponse;
import in.skillsync.skill.entity.Skill;
import in.skillsync.skill.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Skill Service Unit Tests")
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillService skillService;

    // Helper method to bypass the missing AllArgsConstructor
    private SkillRequest createSkillRequest(String name, String category, String description) {
        SkillRequest request = new SkillRequest();
        request.setName(name);
        request.setCategory(category);
        request.setDescription(description);
        return request;
    }

    @Test
    @DisplayName("createSkill - Success")
    void createSkill_Success() {
        SkillRequest request = createSkillRequest("Java", "Backend", "Desc");
        Skill savedSkill = Skill.builder().id(1L).name("Java").category("Backend").build();

        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenReturn(savedSkill);

        SkillResponse response = skillService.createSkill(request);

        assertEquals("Java", response.getName());
        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    @DisplayName("createSkill - Duplicate Throws Exception")
    void createSkill_Duplicate_ThrowsException() {
        SkillRequest request = createSkillRequest("Java", "Backend", "Desc");
        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(true);

        assertThrows(DuplicateSkillException.class, () -> skillService.createSkill(request));
        verify(skillRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllSkills - Returns List")
    void getAllSkills_ReturnsList() {
        Skill skill = Skill.builder().id(1L).name("Java").build();
        when(skillRepository.findAll()).thenReturn(List.of(skill));

        List<SkillResponse> responses = skillService.getAllSkills();

        assertEquals(1, responses.size());
        assertEquals("Java", responses.get(0).getName());
    }

    @Test
    @DisplayName("getSkillById - Success")
    void getSkillById_Success() {
        Skill skill = Skill.builder().id(1L).name("Java").build();
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));

        SkillResponse response = skillService.getSkillById(1L);

        assertEquals("Java", response.getName());
    }

    @Test
    @DisplayName("getSkillById - Not Found Throws Exception")
    void getSkillById_NotFound_ThrowsException() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> skillService.getSkillById(99L));
    }

    @Test
    @DisplayName("getSkillsByCategory - Returns List")
    void getSkillsByCategory_ReturnsList() {
        Skill skill = Skill.builder().id(1L).name("Java").category("Backend").build();
        when(skillRepository.findByCategoryIgnoreCase("Backend")).thenReturn(List.of(skill));

        List<SkillResponse> responses = skillService.getSkillsByCategory("Backend");

        assertEquals(1, responses.size());
    }

    @Test
    @DisplayName("updateSkill - Success")
    void updateSkill_Success() {
        SkillRequest request = createSkillRequest("Java 17", "Backend", "Desc");
        Skill existingSkill = Skill.builder().id(1L).name("Java").build();
        
        when(skillRepository.findById(1L)).thenReturn(Optional.of(existingSkill));
        when(skillRepository.save(any(Skill.class))).thenReturn(existingSkill);

        SkillResponse response = skillService.updateSkill(1L, request);

        assertEquals("Java 17", existingSkill.getName()); // Verifies mutation
        verify(skillRepository).save(existingSkill);
    }

    @Test
    @DisplayName("deleteSkill - Success")
    void deleteSkill_Success() {
        when(skillRepository.existsById(1L)).thenReturn(true);
        skillService.deleteSkill(1L);
        verify(skillRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteSkill - Not Found Throws Exception")
    void deleteSkill_NotFound_ThrowsException() {
        when(skillRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> skillService.deleteSkill(99L));
        verify(skillRepository, never()).deleteById(anyLong());
    }
}