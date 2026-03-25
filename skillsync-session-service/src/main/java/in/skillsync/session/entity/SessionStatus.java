package in.skillsync.session.entity;

/**
 * Session lifecycle states.
 * Stored as STRING in DB — immune to reordering bugs.
 *
 * Valid transitions:
 * REQUESTED → ACCEPTED, REJECTED, CANCELLED
 * ACCEPTED  → COMPLETED, CANCELLED
 */
public enum SessionStatus {
    REQUESTED,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED
}
