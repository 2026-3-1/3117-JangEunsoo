package com.jes.devlearn.domain.qna.service;

import com.jes.devlearn.domain.qna.entity.QnaAnswer;
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
public class QnaAnswerReportTargetResolver implements ReportTargetResolver {

    private final QnaAnswerRepository answerRepository;
    private final QnaQuestionRepository questionRepository;

    @Override
    public ReportTargetType supportedType() {
        return ReportTargetType.QNA_ANSWER;
    }

    @Override
    public Optional<Long> findAuthorId(Long targetId) {
        return answerRepository.findById(targetId).map(QnaAnswer::getAuthorId);
    }

    @Override
    @Transactional
    public void deleteTarget(Long targetId) {
        answerRepository.findById(targetId).ifPresent(a -> {
            questionRepository.findById(a.getQuestionId()).ifPresent(q -> q.decreaseAnswerCount());
            answerRepository.delete(a);
        });
    }
}
