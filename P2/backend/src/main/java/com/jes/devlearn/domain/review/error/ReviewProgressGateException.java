package com.jes.devlearn.domain.review.error;

import com.jes.devlearn.global.exception.CustomException;
import lombok.Getter;

@Getter
public class ReviewProgressGateException extends CustomException {

    private final int currentProgressRate;
    private final int requiredRate;

    public ReviewProgressGateException(int currentProgressRate, int requiredRate) {
        super(ReviewErrorCode.REVIEW_PROGRESS_GATE);
        this.currentProgressRate = currentProgressRate;
        this.requiredRate = requiredRate;
    }
}
