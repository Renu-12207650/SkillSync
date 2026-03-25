package in.skillsync.mentor.repository;

import in.skillsync.mentor.entity.MentorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {

    List<MentorAvailability> findByMentorId(Long mentorId);

    void deleteByMentorId(Long mentorId);
}
