package com.jes.devlearn.domain.playback.repository;

import com.jes.devlearn.domain.playback.entity.PlaybackPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaybackPositionRepository extends JpaRepository<PlaybackPosition, Long> {

    Optional<PlaybackPosition> findByEnrollmentIdAndLectureId(Long enrollmentId, Long lectureId);

    List<PlaybackPosition> findAllByEnrollmentIdOrderByUpdatedAtDesc(Long enrollmentId);
}
