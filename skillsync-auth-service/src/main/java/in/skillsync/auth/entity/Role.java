package in.skillsync.auth.entity;

/**
 * User roles for Role-Based Access Control (RBAC).
 * Stored as STRING in database to prevent reordering bugs.
 */
public enum Role {
    ROLE_LEARNER,
    ROLE_MENTOR,
    ROLE_ADMIN
}
