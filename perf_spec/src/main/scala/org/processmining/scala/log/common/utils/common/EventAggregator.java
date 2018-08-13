package org.processmining.scala.log.common.utils.common;

import java.io.Serializable;

/**
 * Interface for event aggregators
 * Both functions must be implemented
 */
public interface EventAggregator extends Serializable{

    /** Aggregates names of activities */
    String aggregate(final String name);

    /** Aggregates names of segments */
    String aggregateSegmentKey(final String originalKey);
}
