package com.placementprep.repository;

import com.placementprep.domain.Topic;

/** Count per topic from a JPQL group-by (constructor projection). */
public record TopicCount(Topic key, Long count) {
}
