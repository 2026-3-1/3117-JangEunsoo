package com.jes.devlearn.domain.playback.service;

import com.jes.devlearn.domain.course.entity.Lecture;
import com.jes.devlearn.domain.course.entity.Section;
import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.course.repository.SectionRepository;
import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.playback.dto.request.PlaybackUpdateRequest;
import com.jes.devlearn.domain.playback.dto.response.PlaybackPositionResponse;
import com.jes.devlearn.domain.playback.dto.response.ResumeResponse;
import com.jes.devlearn.domain.playback.entity.PlaybackPosition;
import com.jes.devlearn.domain.playback.error.PlaybackErrorCode;
import com.jes.devlearn.domain.playback.repository.PlaybackPositionRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaybackService {

    private final PlaybackPositionRepository playbackPositionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public PlaybackPositionResponse upsert(Long userId, PlaybackUpdateRequest req) {
        Enrollment enrollment = requireOwnedEnrollment(userId, req.enrollmentId());
        ensureLectureBelongsToCourse(enrollment.getCourseId(), req.lectureId());

        PlaybackPosition pos = playbackPositionRepository
                .findByEnrollmentIdAndLectureId(req.enrollmentId(), req.lectureId())
                .orElseGet(() -> playbackPositionRepository.save(
                        new PlaybackPosition(req.enrollmentId(), req.lectureId(), req.currentTimeSeconds())
                ));
        pos.updatePosition(req.currentTimeSeconds());
        return PlaybackPositionResponse.from(pos);
    }

    @Transactional(readOnly = true)
    public PlaybackPositionResponse get(Long userId, Long enrollmentId, Long lectureId) {
        requireOwnedEnrollment(userId, enrollmentId);
        return playbackPositionRepository.findByEnrollmentIdAndLectureId(enrollmentId, lectureId)
                .map(PlaybackPositionResponse::from)
                .orElseGet(() -> new PlaybackPositionResponse(enrollmentId, lectureId, 0, null));
    }

    @Transactional(readOnly = true)
    public ResumeResponse resume(Long userId, Long enrollmentId) {
        Enrollment enrollment = requireOwnedEnrollment(userId, enrollmentId);
        List<PlaybackPosition> positions = playbackPositionRepository
                .findAllByEnrollmentIdOrderByUpdatedAtDesc(enrollmentId);
        if (!positions.isEmpty()) {
            PlaybackPosition latest = positions.get(0);
            return new ResumeResponse(enrollmentId, enrollment.getCourseId(),
                    latest.getLectureId(), latest.getCurrentTimeSeconds());
        }
        Lecture firstLecture = findFirstLecture(enrollment.getCourseId());
        if (firstLecture == null) {
            return new ResumeResponse(enrollmentId, enrollment.getCourseId(), null, 0);
        }
        return new ResumeResponse(enrollmentId, enrollment.getCourseId(), firstLecture.getId(), 0);
    }

    private Enrollment requireOwnedEnrollment(Long userId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new CustomException(PlaybackErrorCode.ENROLLMENT_NOT_FOUND));
        if (!enrollment.getUserId().equals(userId)) {
            throw new CustomException(PlaybackErrorCode.NOT_ENROLLED);
        }
        return enrollment;
    }

    private void ensureLectureBelongsToCourse(Long courseId, Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(PlaybackErrorCode.LECTURE_NOT_IN_COURSE));
        Section section = sectionRepository.findById(lecture.getSectionId())
                .orElseThrow(() -> new CustomException(PlaybackErrorCode.LECTURE_NOT_IN_COURSE));
        if (!section.getCourseId().equals(courseId)) {
            throw new CustomException(PlaybackErrorCode.LECTURE_NOT_IN_COURSE);
        }
    }

    private Lecture findFirstLecture(Long courseId) {
        List<Section> sections = sectionRepository.findAllByCourseIdOrderByOrderNumAsc(courseId);
        for (Section s : sections) {
            List<Lecture> lectures = lectureRepository.findAllBySectionIdOrderByOrderNumAsc(s.getId());
            if (!lectures.isEmpty()) {
                return lectures.stream()
                        .min(Comparator.comparing(l -> l.getOrderNum() == null ? Integer.MAX_VALUE : l.getOrderNum()))
                        .orElse(null);
            }
        }
        return null;
    }
}
