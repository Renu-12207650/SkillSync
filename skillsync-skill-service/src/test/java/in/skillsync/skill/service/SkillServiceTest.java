package in.skillsync.skill.service;

import in.skillsync.common.exception.DuplicateSkillException;
import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.skill.dto.SkillRequest;
import in.skillsync.skill.entity.Skill;
import in.skillsync.skill.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillService Unit Tests")
class SkillServiceTest {

    @Mock private SkillRepository skillRepository;
    @InjectMocks private SkillService skillService;

    private SkillRequest skillRequest;
    private Skill savedSkill;

    @BeforeEach
    void setUp() {
        skillRequest = new SkillRequest();
        skillRequest.setName("Spring Boot");
        skillRequest.setCategory("Backend");
        skillRequest.setDescription("Java backend framework");

        savedSkill = Skill.builder()
                .id(1L)
                .name("Spring Boot")
                .category("Backend")
                .description("Java backend framework")
                .build();
    }

    @Test
    @DisplayName("createSkill - success - returns SkillResponse")
    void createSkill_success_returnsResponse() {
        when(skillRepository.existsByNameIgnoreCase("Spring Boot")).thenReturn(false);
        when(skillRepository.save(any())).thenReturn(savedSkill);

        var response = skillService.createSkill(skillRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Spring Boot");
        assertThat(response.getCategory()).isEqualTo("Backend");
        verify(skillRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createSkill - duplicate name - throws DuplicateSkillException")
    void createSkill_duplicateName_throwsException() {
        when(skillRepository.existsByNameIgnoreCase("Spring Boot")).thenReturn(true);

        assertThatThrownBy(() -> skillService.createSkill(skillRequest))
                .isInstanceOf(DuplicateSkillException.class)
                .hasMessageContaining("Skill already exists");

        verify(skillRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllSkills - returns list of skills")
    void getAllSkills_returnsSkillList() {
        when(skillRepository.findAll()).thenReturn(List.of(savedSkill));

        var skills = skillService.getAllSkills();

        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).getName()).isEqualTo("Spring Boot");
    }

    @Test
    @DisplayName("getSkillById - not found - throws ResourceNotFoundException")
    void getSkillById_notFound_throwsException() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.getSkillById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
