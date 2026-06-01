package com.jes.devlearn.domain.qna.repository;

import com.jes.devlearn.domain.qna.entity.QnaQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaQuestionRepository extends JpaRepository<QnaQuestion, Long> {

    Page<QnaQuestion> findAllByCourseIdOrderByIdDesc(Long courseId, Pageable pageable);

    long countByCourseId(Long courseId);
}
