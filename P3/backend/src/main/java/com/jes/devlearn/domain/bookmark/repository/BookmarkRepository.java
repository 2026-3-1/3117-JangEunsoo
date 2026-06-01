package com.jes.devlearn.domain.bookmark.repository;

import com.jes.devlearn.domain.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Bookmark> findAllByUserIdAndLectureIdOrderByTimeSecondsAsc(Long userId, Long lectureId);

    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);
}
