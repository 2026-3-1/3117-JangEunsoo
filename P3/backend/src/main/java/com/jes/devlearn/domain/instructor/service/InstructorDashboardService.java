package com.jes.devlearn.domain.instructor.service;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.order.repository.OrderItemRepository;
import com.jes.devlearn.domain.instructor.dto.response.InstructorCourseStudentsResponse;
import com.jes.devlearn.domain.instructor.dto.response.InstructorDashboardResponse;
import com.jes.devlearn.domain.progress.repository.LectureProgressRepository;
import com.jes.devlearn.domain.review.repository.ReviewRepository;
import com.jes.devlearn.domain.user.entity.User;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.security.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InstructorDashboardService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final UserRepository userRepository;
    private final OwnershipValidator ownershipValidator;

    @Transactional(readOnly = true)
    public InstructorDashboardResponse getDashboard(Long instructorId) {
        long total = courseRepository.countByInstructorId(instructorId);
        long published = courseRepository.countByInstructorIdAndPublishStatus(instructorId, PublishStatus.PUBLISHED);
        long draft = courseRepository.countByInstructorIdAndPublishStatus(instructorId, PublishStatus.DRAFT);
        long archived = courseRepository.countByInstructorIdAndPublishStatus(instructorId, PublishStatus.ARCHIVED);

        List<Course> myCourses = courseRepository.findAllByInstructorIdOrderByIdDesc(instructorId);
        List<Long> courseIds = myCourses.stream().map(Course::getId).toList();

        long totalEnrollments = courseIds.isEmpty() ? 0 : enrollmentRepository.countByCourseIdIn(courseIds);
        long totalReviews = courseIds.isEmpty() ? 0 : reviewRepository.countByCourseIdIn(courseIds);
        double averageRating = courseIds.isEmpty() ? 0.0
                : (reviewRepository.avgRatingByCourseIds(courseIds) == null ? 0.0 : reviewRepository.avgRatingByCourseIds(courseIds));

        long totalRevenue = courseIds.isEmpty() ? 0 : orderItemRepository.sumActiveRevenueByCourseIds(courseIds);

        List<Enrollment> recent = courseIds.isEmpty()
                ? List.of()
                : enrollmentRepository.findRecentByCourseIds(courseIds, PageRequest.of(0, 5));

        Map<Long, String> titleByCourseId = new HashMap<>();
        myCourses.forEach(c -> titleByCourseId.put(c.getId(), c.getTitle()));

        Map<Long, User> usersById = new HashMap<>();
        recent.stream().map(Enrollment::getUserId).distinct().forEach(uid ->
                userRepository.findById(uid).ifPresent(u -> usersById.put(uid, u))
        );

        List<InstructorDashboardResponse.RecentEnrollmentItem> recentItems = recent.stream()
                .map(e -> new InstructorDashboardResponse.RecentEnrollmentItem(
                        e.getId(),
                        e.getCourseId(),
                        titleByCourseId.getOrDefault(e.getCourseId(), null),
                        e.getUserId(),
                        usersById.containsKey(e.getUserId()) ? usersById.get(e.getUserId()).getUsername() : null,
                        e.getCreatedAt()
                ))
                .toList();

        return new InstructorDashboardResponse(
                total, published, draft, archived,
                totalEnrollments, totalReviews,
                Math.round(averageRating * 10.0) / 10.0,
                totalRevenue,
                recentItems
        );
    }

    @Transactional(readOnly = true)
    public InstructorCourseStudentsResponse getCourseStudents(Long instructorId, Long courseId) {
        ownershipValidator.requireOwnedCourse(courseId, instructorId);
        long totalLectures = lectureRepository.countByCourseId(courseId);
        List<Enrollment> enrollments = enrollmentRepository.findAllByCourseId(courseId);

        Map<Long, User> userMap = new HashMap<>();
        enrollments.stream().map(Enrollment::getUserId).distinct().forEach(uid ->
                userRepository.findById(uid).ifPresent(u -> userMap.put(uid, u))
        );

        List<InstructorCourseStudentsResponse.StudentItem> items = enrollments.stream()
                .map(e -> {
                    long completed = lectureProgressRepository.countByEnrollmentId(e.getId());
                    int rate = totalLectures == 0 ? 0 : (int) Math.round((completed * 100.0) / totalLectures);
                    User u = userMap.get(e.getUserId());
                    return new InstructorCourseStudentsResponse.StudentItem(
                            e.getId(),
                            e.getUserId(),
                            u == null ? null : u.getUsername(),
                            e.getCreatedAt(),
                            completed,
                            rate
                    );
                })
                .toList();

        return new InstructorCourseStudentsResponse(courseId, totalLectures, items);
    }
}
