package com.jes.devlearn.domain.instructor.service;

import com.jes.devlearn.domain.course.entity.Section;
import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.course.repository.SectionRepository;
import com.jes.devlearn.domain.instructor.dto.request.InstructorSectionRequest;
import com.jes.devlearn.domain.instructor.dto.response.InstructorSectionResponse;
import com.jes.devlearn.domain.instructor.error.InstructorErrorCode;
import com.jes.devlearn.global.exception.CustomException;
import com.jes.devlearn.global.security.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstructorSectionService {

    private final SectionRepository sectionRepository;
    private final LectureRepository lectureRepository;
    private final OwnershipValidator ownershipValidator;

    @Transactional(readOnly = true)
    public List<InstructorSectionResponse> list(Long instructorId, Long courseId) {
        ownershipValidator.requireOwnedCourse(courseId, instructorId);
        return sectionRepository.findAllByCourseIdOrderByOrderNumAsc(courseId).stream()
                .map(InstructorSectionResponse::from)
                .toList();
    }

    @Transactional
    public InstructorSectionResponse create(Long instructorId, Long courseId, InstructorSectionRequest req) {
        ownershipValidator.requireOwnedCourse(courseId, instructorId);
        Integer orderNum = req.orderNum();
        if (orderNum == null) {
            orderNum = sectionRepository.findTopByCourseIdOrderByOrderNumDesc(courseId)
                    .map(s -> (s.getOrderNum() == null ? 0 : s.getOrderNum()) + 1)
                    .orElse(1);
        }
        Section section = new Section(courseId, req.title(), orderNum);
        sectionRepository.save(section);
        return InstructorSectionResponse.from(section);
    }

    @Transactional
    public InstructorSectionResponse update(Long instructorId, Long courseId, Long sectionId, InstructorSectionRequest req) {
        ownershipValidator.requireOwnedCourse(courseId, instructorId);
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new CustomException(InstructorErrorCode.SECTION_NOT_FOUND));
        if (!section.getCourseId().equals(courseId)) {
            throw new CustomException(InstructorErrorCode.SECTION_NOT_FOUND);
        }
        section.update(req.title(), req.orderNum());
        return InstructorSectionResponse.from(section);
    }

    @Transactional
    public void delete(Long instructorId, Long courseId, Long sectionId) {
        ownershipValidator.requireOwnedCourse(courseId, instructorId);
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new CustomException(InstructorErrorCode.SECTION_NOT_FOUND));
        if (!section.getCourseId().equals(courseId)) {
            throw new CustomException(InstructorErrorCode.SECTION_NOT_FOUND);
        }
        lectureRepository.deleteAllBySectionId(sectionId);
        sectionRepository.delete(section);
    }
}
