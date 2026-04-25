package com.jes.devlearn.domain.bookmark.service;

import com.jes.devlearn.domain.bookmark.dto.request.BookmarkCreateRequest;
import com.jes.devlearn.domain.bookmark.dto.request.BookmarkUpdateRequest;
import com.jes.devlearn.domain.bookmark.dto.response.BookmarkResponse;
import com.jes.devlearn.domain.bookmark.entity.Bookmark;
import com.jes.devlearn.domain.bookmark.error.BookmarkErrorCode;
import com.jes.devlearn.domain.bookmark.repository.BookmarkRepository;
import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.Lecture;
import com.jes.devlearn.domain.course.entity.Section;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.course.repository.SectionRepository;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final LectureRepository lectureRepository;
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public BookmarkResponse create(Long userId, BookmarkCreateRequest req) {
        Long courseId = courseIdOfLecture(req.lectureId());
        ensureEnrolled(userId, courseId);
        Bookmark bookmark = bookmarkRepository.save(new Bookmark(userId, req.lectureId(), req.timeSeconds(), req.memo()));
        return toResponse(bookmark);
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> listMine(Long userId) {
        List<Bookmark> bookmarks = bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return enrich(bookmarks);
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> listByLecture(Long userId, Long lectureId) {
        Long courseId = courseIdOfLecture(lectureId);
        ensureEnrolled(userId, courseId);
        return enrich(bookmarkRepository.findAllByUserIdAndLectureIdOrderByTimeSecondsAsc(userId, lectureId));
    }

    @Transactional
    public BookmarkResponse update(Long userId, Long bookmarkId, BookmarkUpdateRequest req) {
        Bookmark bookmark = bookmarkRepository.findByIdAndUserId(bookmarkId, userId)
                .orElseThrow(() -> new CustomException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));
        bookmark.update(req.timeSeconds(), req.memo());
        return toResponse(bookmark);
    }

    @Transactional
    public void delete(Long userId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findByIdAndUserId(bookmarkId, userId)
                .orElseThrow(() -> new CustomException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));
        bookmarkRepository.delete(bookmark);
    }

    private Long courseIdOfLecture(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        Section section = sectionRepository.findById(lecture.getSectionId())
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        return section.getCourseId();
    }

    private void ensureEnrolled(Long userId, Long courseId) {
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new CustomException(BookmarkErrorCode.NOT_ENROLLED);
        }
    }

    private BookmarkResponse toResponse(Bookmark bookmark) {
        Lecture lecture = lectureRepository.findById(bookmark.getLectureId()).orElse(null);
        if (lecture == null) {
            return BookmarkResponse.of(bookmark, null, null, null);
        }
        Section section = sectionRepository.findById(lecture.getSectionId()).orElse(null);
        Long courseId = section == null ? null : section.getCourseId();
        Course course = courseId == null ? null : courseRepository.findById(courseId).orElse(null);
        return BookmarkResponse.of(
                bookmark,
                lecture.getTitle(),
                courseId,
                course == null ? null : course.getTitle()
        );
    }

    private List<BookmarkResponse> enrich(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) return List.of();
        List<Long> lectureIds = bookmarks.stream().map(Bookmark::getLectureId).distinct().toList();
        Map<Long, Lecture> lectureMap = new HashMap<>();
        lectureRepository.findAllById(lectureIds).forEach(l -> lectureMap.put(l.getId(), l));

        List<Long> sectionIds = lectureMap.values().stream().map(Lecture::getSectionId).distinct().toList();
        Map<Long, Section> sectionMap = new HashMap<>();
        sectionRepository.findAllById(sectionIds).forEach(s -> sectionMap.put(s.getId(), s));

        List<Long> courseIds = sectionMap.values().stream().map(Section::getCourseId).distinct().toList();
        Map<Long, Course> courseMap = new HashMap<>();
        courseRepository.findAllById(courseIds).forEach(c -> courseMap.put(c.getId(), c));

        return bookmarks.stream().map(b -> {
            Lecture l = lectureMap.get(b.getLectureId());
            String lectureTitle = l == null ? null : l.getTitle();
            Long courseId = null;
            String courseTitle = null;
            if (l != null) {
                Section s = sectionMap.get(l.getSectionId());
                if (s != null) {
                    courseId = s.getCourseId();
                    Course c = courseMap.get(courseId);
                    courseTitle = c == null ? null : c.getTitle();
                }
            }
            return BookmarkResponse.of(b, lectureTitle, courseId, courseTitle);
        }).toList();
    }
}
