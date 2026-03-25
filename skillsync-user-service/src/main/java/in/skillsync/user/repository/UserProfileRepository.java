package in.skillsync.user.repository;

import in.skillsync.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);
}
