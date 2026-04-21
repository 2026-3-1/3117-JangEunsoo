package com.jes.devlearn.domain.instructor.entity;

import com.jes.devlearn.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "instructor_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InstructorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "career_years")
    private Integer careerYears;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public InstructorProfile(User user, String displayName, String bio, Integer careerYears, String profileImageUrl) {
        this.user = user;
        this.displayName = displayName;
        this.bio = bio;
        this.careerYears = careerYears;
        this.profileImageUrl = profileImageUrl;
    }

    public void update(String displayName, String bio, Integer careerYears, String profileImageUrl) {
        this.displayName = displayName;
        this.bio = bio;
        this.careerYears = careerYears;
        this.profileImageUrl = profileImageUrl;
    }
}
