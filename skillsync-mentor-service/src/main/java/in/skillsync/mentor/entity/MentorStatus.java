package in.skillsync.mentor.entity;

/**
 * Lifecycle states for mentor approval workflow.
 * Stored as STRING in DB — immune to reordering bugs.
 */
public enum MentorStatus {
    PENDING_APPROVAL,
    ACTIVE,
    INACTIVE,
    REJECTED
}
