package org.processmininginlogistics.classifiers.bp.example;

import org.processmining.scala.log.common.enhancment.segments.common.AbstractDurationClassifier;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * README
 * 1. Use the PSM jar as a dependency and implement your classifier.
 * It must implement AbstractDurationClassifier and has a constructor with a single String argument.
 * This argument is an event log(s) path, or an empty string in case of using imported XES log in ProM
 * 2. Provide a readable name of the classifier in toString()
 * 3. Compile your implementation as a separate jar
 * 4. Put it into the CLASSPATH of the PSM
 * 5. Run the PSM
 * 6. Provide a full class name in field 'custom classifier' in the pre-processing dialog
 *     e.g. org.processmininginlogistics.classifiers.bp.example.SegmentClassifierExample
 * 7. Check the legend in the PSM. It should show your classifier name and classes
 * <p>
 * If you work with the PSM from an IDE, it may be easier to implement your classifier as a part of the PSM.
 * Do not commit non-generic or containing proprietary information classifiers to the PSM!
 */
public class SegmentClassifierExample extends AbstractDurationClassifier {

    public SegmentClassifierExample(final String path) {

    }

    @Override
    public int classify(long duration, double q2, double median, double q4, String caseId, long timestamp, String segmentName, double medianAbsDeviation) {
        return duration <= median ? 0 : 1;
    }

    @Override
    public String sparkSqlExpression(String attrNameDuration, String attrNameClazz) {
        throw new NotImplementedException();
    }

    @Override
    public String legend() {
        return "Example%Class_0%Class_1";
    }

    @Override
    public int classCount() {
        return 2;
    }

    @Override
    public String toString() {
        return "Segment/case classifier example";
    }
}
