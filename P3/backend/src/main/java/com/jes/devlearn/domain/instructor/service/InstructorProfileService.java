package com.jes.devlearn.domain.instructor.service;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.instructor.dto.request.InstructorProfileUpdateRequest;
import com.jes.devlearn.domain.instructor.dto.response.InstructorProfileResponse;
import com.jes.devlearn.domain.instructor.dto.response.InstructorPublicProfileResponse;
import com.jes.devlearn.domain.instructor.entity.InstructorProfile;
import com.jes.devlearn.domain.instructor.error.InstructorErrorCode;
import com.jes.devlearn.domain.instructor.repository.InstructorProfileRepository;
import com.jes.devlearn.domain.review.repository.ReviewRepository;
import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.domain.user.entity.User;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstructorProfileService {

    private final InstructorProfileRepository instructorProfileRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public InstructorProfileResponse getMine(Long userId) {
        InstructorProfile profile = instructorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(InstructorErrorCode.PROFILE_NOT_FOUND));
        return InstructorProfileResponse.from(profile);
    }

    @Transactional
    public InstructorProfileResponse upsertMine(Long userId, InstructorProfileUpdateRequest req) {
        InstructorProfile profile = instructorProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomException(InstructorErrorCode.NOT_INSTRUCTOR));
                    InstructorProfile newProfile = new InstructorProfile(
                            user, req.displayName(), req.bio(), req.careerYears(), req.profileImageUrl());
                    return instructorProfileRepository.save(newProfile);
                });
        profile.update(req.displayName(), req.bio(), req.careerYears(), req.profileImageUrl());
        return InstructorProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public InstructorPublicProfileResponse getPublic(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(InstructorErrorCode.PROFILE_NOT_FOUND));
        if (user.getRole() != Role.INSTRUCTOR) {
            throw new CustomException(InstructorErrorCode.PROFILE_NOT_FOUND);
        }
        InstructorProfile profile = instructorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(InstructorErrorCode.PROFILE_NOT_FOUND));

        List<Course> publishedCourses = courseRepository.findAllByInstructorIdAndPublishStatus(userId, PublishStatus.PUBLISHED);
        List<Long> courseIds = publishedCourses.stream().map(Course::getId).toList();

        long totalEnrollments = courseIds.isEmpty() ? 0 : enrollmentRepository.countByCourseIdIn(courseIds);
        long totalReviews = courseIds.isEmpty() ? 0 : reviewRepository.countByCourseIdIn(courseIds);
        double averageRating = courseIds.isEmpty()
                ? 0.0
                : (reviewRepository.avgRatingByCourseIds(courseIds) == null ? 0.0 : reviewRepository.avgRatingByCourseIds(courseIds));

        List<InstructorPublicProfileResponse.PublicCourseItem> items = publishedCourses.stream()
                .map(c -> {
                    long ec = enrollmentRepository.countByCourseId(c.getId());
                    Double avg = reviewRepository.avgRatingByCourseId(c.getId());
                    return InstructorPublicProfileResponse.PublicCourseItem.from(c, ec, avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0);
                })
                .toList();

        return InstructorPublicProfileResponse.of(
                profile, totalEnrollments, totalReviews,
                Math.round(averageRating * 10.0) / 10.0, items
        );
    }
}
