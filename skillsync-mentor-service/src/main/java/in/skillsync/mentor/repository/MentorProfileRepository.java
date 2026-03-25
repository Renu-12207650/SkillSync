package in.skillsync.mentor.repository;

import in.skillsync.mentor.entity.MentorProfile;
import in.skillsync.mentor.entity.MentorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long> {

    Optional<MentorProfile> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);

    Page<MentorProfile> findByStatus(MentorStatus status, Pageable pageable);

    /**
     * Search active mentors with optional filters.
     * All parameters are optional — null means the filter is skipped.
     */
    @Query("SELECT m FROM MentorProfile m WHERE m.status = 'ACTIVE' " +
           "AND (:skillId IS NULL OR :skillId MEMBER OF m.skillIds) " +
           "AND (:minRating IS NULL OR m.averageRating >= :minRating) " +
           "AND (:maxRate IS NULL OR m.hourlyRate <= :maxRate) " +
           "AND (:minExp IS NULL OR m.yearsOfExperience >= :minExp)")
    Page<MentorProfile> searchActiveMentors(
            @Param("skillId")   Long skillId,
            @Param("minRating") BigDecimal minRating,
            @Param("maxRate")   BigDecimal maxRate,
            @Param("minExp")    Integer minExp,
            Pageable pageable);
}
