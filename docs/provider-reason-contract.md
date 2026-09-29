# Provider justification: reason

Implemented 2026-09-27. This supersedes reasonCode/reasonDescription for the provider's justification.

## Contract

- `reason` is optional free text supplied by the provider, stored as a nullable PostgreSQL TEXT column on the payer review, and returned verbatim in the review-queue item.
- It may describe symptoms, observations, diagnoses, or other evidence. It is not a condition identifier and needs no PatientCondition lookup.
- `escalationReason` remains the payer's structured explanation for physician review. `decisionReason` remains the explanation for the final decision.
- Automatic approval continues to evaluate condition-based criteria independently of the narrative.
- Structured supporting evidence can be added later alongside `reason`.

## Implemented code

- PriorAuthReview accepts reason as the final argument of a new constructor overload. The existing constructor remains available and defaults reason to null.
- ReviewItem exposes reason instead of reasonCode and reasonDescription.
- ClinicalReviewService maps review.getReason() directly into the queue DTO. The batch condition-description lookup and its repository finder were removed.
- V4 renames reason_code to reason and changes its type to TEXT. Existing text and nulls are retained verbatim; old code values are not translated into fabricated descriptions.
- Existing migrations remain unchanged. V4 has not been applied to a running database as part of this change.

## Coordinated contract updates published in Notion

With explicit user approval, the following updates were published and verified on 2026-09-27 in 03 Payer Service, 04 Provider Service, 05 Kafka Contracts, and PA-20. Story statuses were not changed.

### 03 Payer Service / PA-20

Replace the schema's nullable reason_code with nullable text reason. Replace reasonCode/reasonDescription in the queue JSON with reason. Revise PA-20 to return the stored narrative directly rather than joining a reason description.

### 04 Provider Service

Replace reason_code in the future authRequest schema and reasonCode in POST /api/v1/requests with reason. Store the optional narrative unchanged. Remove the condition-membership validation REASON_CODE_NOT_ON_PATIENT. The patient-conditions endpoint provides clinical history rather than a required reason-code selection. Appeals copy the parent's reason.

### 05 Kafka Contracts

RequestSubmitted carries optional reason (string or null) instead of reasonCode. The payer copies it unchanged into PriorAuthReview. Intake-result reason, escalationReason, and decisionReason keep their existing meanings.

The provider service and Kafka authRequest handlers do not yet exist in this checkout; these are contracts for their later implementation, not completed runtime paths.

## Verification

41 focused unit-test cases passed across ClinicalEvaluatorTest, ClinicalReviewServiceTest, DecisionServiceTest, and IntakeValidationServiceTest. Tests include optional/multiline reason mapping without condition queries and preservation of the provider narrative during clinical review. They do not validate a live migration or complete PA-20 endpoint behavior.
