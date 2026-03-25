package in.skillsync.skill.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Centralised skill catalogue entity.
 * Created by admins; referenced by mentors via skillIds set.
 * Table: skills in skillsync_skill_db
 */
@Entity
@Table(
    name = "skills",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "name", name = "uk_skills_name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Skill name is required")
    @Size(min = 2, max = 100, message = "Skill name must be between 2 and 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String category;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(length = 500)
    private String description;
}
