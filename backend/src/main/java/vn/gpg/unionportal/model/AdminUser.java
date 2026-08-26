package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "admin_users")
@Getter
@Setter
@NoArgsConstructor
public class AdminUser extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 30)
    private String role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "union_unit_id")
    private UnionUnit unionUnit;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "active_token_id", length = 36)
    private String activeTokenId;
}
