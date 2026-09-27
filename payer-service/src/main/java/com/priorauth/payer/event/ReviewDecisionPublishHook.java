package com.priorauth.payer.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReviewDecisionPublishHook {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewDecided(ReviewDecidedEvent event) {
        // Sprint 9: publish the decision event to Kafka.
    }
}
