package com.jes.devlearn.domain.instructor.service;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.course.repository.SectionRepository;
import com.jes.devlearn.domain.instructor.dto.request.InstructorCourseCreateRequest;
import com.jes.devlearn.domain.instructor.dto.request.InstructorCourseUpdateRequest;
import com.jes.devlearn.domain.instructor.dto.response.InstructorCourseResponse;
import com.jes.devlearn.domain.instructor.error.InstructorErrorCode;
import com.jes.devlearn.domain.user.entity.User;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.exception.CustomException;
import com.jes.devlearn.global.security.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstructorCourseService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final OwnershipValidator ownershipValidator;

    @Transactional
    public InstructorCourseResponse create(Long instructorId, InstructorCourseCreateRequest req) {
        User user = userRepository.findById(instructorId)
                .orElseThrow(() -> new CustomException(InstructorErrorCode.NOT_INSTRUCTOR));
        Course course = new Course(
                instructorId,
                req.categoryId(),
                req.title(),
                req.description(),
                req.difficulty(),
                user.getUsername(),
                req.price() == null ? 0L : req.price()
        );
        courseRepository.save(course);
        return InstructorCourseResponse.from(course);
    }

    @Transactional(readOnly = true)
    public List<InstructorCourseResponse> listMyCourses(Long instructorId, PublishStatus status) {
        List<Course> courses = (status == null)
                ? courseRepository.findAllByInstructorIdOrderByIdDesc(instructorId)
                : courseRepository.findAllByInstructorIdAndPublishStatusOrderByIdDesc(instructorId, status);
        return courses.stream().map(InstructorCourseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InstructorCourseResponse getMyCourse(Long instructorId, Long courseId) {
        Course course = ownershipValidator.requireOwnedCourse(courseId, instructorId);
        return InstructorCourseResponse.from(course);
    }

    @Transactional
    public InstructorCourseResponse update(Long instructorId, Long courseId, InstructorCourseUpdateRequest req) {
        Course course = ownershipValidator.requireOwnedCourse(courseId, instructorId);
        course.updateInstructorEdit(req.categoryId(), req.title(), req.description(), req.difficulty(), req.price());
        return InstructorCourseResponse.from(course);
    }

    @Transactional
    public void archive(Long instructorId, Long courseId) {
        Course course = ownershipValidator.requireOwnedCourse(courseId, instructorId);
        if (course.getPublishStatus() == PublishStatus.ARCHIVED) {
            throw new CustomException(InstructorErrorCode.ALREADY_ARCHIVED);
        }
        course.archive();
    }

    @Transactional
    public InstructorCourseResponse publish(Long instructorId, Long courseId) {
        Course course = ownershipValidator.requireOwnedCourse(courseId, instructorId);
        if (course.getPublishStatus() == PublishStatus.PUBLISHED) {
            throw new CustomException(InstructorErrorCode.ALREADY_PUBLISHED);
        }
        long sectionCount = sectionRepository.countByCourseId(courseId);
        long lectureCount = lectureRepository.countByCourseId(courseId);
        if (sectionCount < 1 || lectureCount < 1) {
            throw new CustomException(InstructorErrorCode.PUBLISH_VALIDATION_FAILED);
        }
        course.publish();
        return InstructorCourseResponse.from(course);
    }

    @Transactional
    public void delete(Long instructorId, Long courseId) {
        Course course = ownershipValidator.requireOwnedCourse(courseId, instructorId);
        if (course.getPublishStatus() == PublishStatus.PUBLISHED) {
            course.archive();
            return;
        }
        courseRepository.delete(course);
    }
}
