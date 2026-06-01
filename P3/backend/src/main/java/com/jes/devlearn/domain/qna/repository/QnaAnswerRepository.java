package com.jes.devlearn.domain.qna.repository;

import com.jes.devlearn.domain.qna.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {

    List<QnaAnswer> findAllByQuestionIdOrderByIdAsc(Long questionId);

    List<QnaAnswer> findAllByQuestionIdIn(Collection<Long> questionIds);

    void deleteAllByQuestionId(Long questionId);
}
