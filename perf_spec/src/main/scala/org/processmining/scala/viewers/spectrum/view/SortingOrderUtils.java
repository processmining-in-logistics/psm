package org.processmining.scala.viewers.spectrum.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class SortingOrderUtils{
    private static final Logger logger = LoggerFactory.getLogger(SortingOrderUtils.class.getName());
    public static String[] readSortingOrder(final String filename) throws IOException {
        final java.util.List<String> list =
                Files
                        .lines(Paths.get(filename))
                        .map(x -> x.trim())
                        .filter(x -> !x.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
        logger.info("Sorting order:");
        list.forEach(logger::info);
        return list.toArray(new String[list.size()]);
    }

    public static String[] readSortingOrderNonDistinct(final String filename) throws IOException {
        final java.util.List<String> list =
                Files
                        .lines(Paths.get(filename))
                        .map(x -> x.trim())
                        .filter(x -> !x.isEmpty())
                        .collect(Collectors.toList());
        logger.info("Sorting order:");
        list.forEach(logger::info);
        return list.toArray(new String[list.size()]);
    }



}