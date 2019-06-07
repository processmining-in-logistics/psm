package org.processmining.scala.viewers.spectrum.api;

public interface PsmApi {
    void sortAndFilter(String[] sortedSegments);

    void addEventHandler(PsmEvents handler);

    void removeEventHandler(PsmEvents handler);
}
