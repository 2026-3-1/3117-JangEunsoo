package com.jes.devlearn.domain.instructor.service;

import com.jes.devlearn.domain.course.entity.Lecture;
import com.jes.devlearn.domain.course.entity.Section;
import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.course.repository.SectionRepository;
import com.jes.devlearn.domain.instructor.dto.request.InstructorLectureRequest;
import com.jes.devlearn.domain.instructor.dto.response.InstructorLectureResponse;
import com.jes.devlearn.domain.instructor.error.InstructorErrorCode;
import com.jes.devlearn.global.exception.CustomException;
import com.jes.devlearn.global.security.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstructorLectureService {

    private final SectionRepository sectionRepository;
    private final LectureRepository lectureRepository;
    private final OwnershipValidator ownershipValidator;

    @Transactional(readOnly = true)
    public List<InstructorLectureResponse> list(Long instructorId, Long courseId, Long sectionId) {
        ownershipValidator.requireOwnedCourse(courseId, instructorId);
        Section section = requireSection(courseId, sectionId);
        return lectureRepository.findAllBySectionIdOrderByOrderNumAsc(section.getId()).stream()
                .map(InstructorLectureResponse::from)
                .toList();
    }

    @Transactional
    public InstructorLectureResponse create(Long instructorId, Long courseId, Long sectionId, InstructorLectureRequest req) {
        ownershipValidator.requireOwnedCourse(courseId, instructorId);
        Section section = requireSection(courseId, sectionId);
        Integer orderNum = req.orderNum();
        if (orderNum == null) {
            orderNum = lectureRepository.findTopBySectionIdOrderByOrderNumDesc(section.getId())
                    .map(l -> (l.getOrderNum() == null ? 0 : l.getOrderNum()) + 1)
                    .orElse(1);
        }
        Lecture lecture = new Lecture(section.getId(), req.title(), req.videoUrl(), orderNum, req.durationSeconds());
        lectureRepository.save(lecture);
        return InstructorLectureResponse.from(lecture);
    }

    @Transactional
    public InstructorLectureResponse update(Long instructorId, Long courseId, Long sectionId, Long lectureId, InstructorLectureRequest req) {
        ownershipValidator.requireOwnedCourse(courseId, instructorId);
        Section section = requireSection(courseId, sectionId);
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(InstructorErrorCode.LECTURE_NOT_FOUND));
        if (!lecture.getSectionId().equals(section.getId())) {
            throw new CustomException(InstructorErrorCode.LECTURE_NOT_FOUND);
        }
        lecture.update(req.title(), req.videoUrl(), req.orderNum(), req.durationSeconds());
        return InstructorLectureResponse.from(lecture);
    }

    @Transactional
    public void delete(Long instructorId, Long courseId, Long sectionId, Long lectureId) {
        ownershipValidator.requireOwnedCourse(courseId, instructorId);
        Section section = requireSection(courseId, sectionId);
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(InstructorErrorCode.LECTURE_NOT_FOUND));
        if (!lecture.getSectionId().equals(section.getId())) {
            throw new CustomException(InstructorErrorCode.LECTURE_NOT_FOUND);
        }
        lectureRepository.delete(lecture);
    }

    private Section requireSection(Long courseId, Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new CustomException(InstructorErrorCode.SECTION_NOT_FOUND));
        if (!section.getCourseId().equals(courseId)) {
            throw new CustomException(InstructorErrorCode.SECTION_NOT_FOUND);
        }
        return section;
    }
}
