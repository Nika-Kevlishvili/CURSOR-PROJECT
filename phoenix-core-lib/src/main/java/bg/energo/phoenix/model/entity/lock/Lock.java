package bg.energo.phoenix.model.entity.lock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Entity
@Table(schema = "lock", name = "locks", uniqueConstraints = @UniqueConstraint(columnNames = "lock_key"))
public class Lock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lock_key", nullable = false, unique = true)
    private String lockKey;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "lock_owner", nullable = false)
    private String lockOwner;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at",nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "has_super_owner", nullable = false)
    private boolean hasSuperOwner;

    @Column(name = "system_lock")
    private boolean systemLock;

    @Column(name = "billing_id")
    BigInteger billingId;

    @Version
    private Integer version;

    /**
     * Checks if the lock has expired based on the current timestamp.
     *
     * @return true if the lock has expired or the expiration is set and reached, false otherwise.
     */
    public boolean isExpired() {
        return LocalDateTime.now(ZoneId.of("UTC")).isAfter(expiresAt);
    }

    /**
     * Generates the lock key based on the entity type and ID.
     *
     * @return a unique lock key string for this lock.
     */
    public String generateLockKey() {
        return generateLockKey(this.entityType, this.entityId, this.versionId);
    }

    public static String generateLockKey(String entityType, Long entityId, Long versionId) {
        return entityType + ":" + entityId + (versionId != null ? ":" + versionId : "");
    }

    @PrePersist
    public void setCreationTimestampAndLockKey() {
        this.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
        if (this.lockKey == null) {
            this.lockKey = generateLockKey();
        }
    }

    public String getFormattedCreatedAt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return this.getCreatedAt().format(formatter);
    }

    public boolean hasSuperOwner() {
        return this.hasSuperOwner;
    }
}
