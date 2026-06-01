package com.jes.devlearn.domain.qna.service;

import com.jes.devlearn.domain.qna.entity.QnaQuestion;
import com.jes.devlearn.domain.qna.repository.QnaAnswerRepository;
import com.jes.devlearn.domain.qna.repository.QnaQuestionRepository;
import com.jes.devlearn.domain.report.entity.ReportTargetType;
import com.jes.devlearn.domain.report.service.ReportTargetResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QnaQuestionReportTargetResolver implements ReportTargetResolver {

    private final QnaQuestionRepository questionRepository;
    private final QnaAnswerRepository answerRepository;

    @Override
    public ReportTargetType supportedType() {
        return ReportTargetType.QNA_QUESTION;
    }

    @Override
    public Optional<Long> findAuthorId(Long targetId) {
        return questionRepository.findById(targetId).map(QnaQuestion::getAuthorId);
    }

    @Override
    @Transactional
    public void deleteTarget(Long targetId) {
        questionRepository.findById(targetId).ifPresent(q -> {
            answerRepository.deleteAllByQuestionId(q.getId());
            questionRepository.delete(q);
        });
    }
}
