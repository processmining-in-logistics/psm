package org.processmining.scala.log.common.utils.common;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;

public final class EventAggregatorImpl implements EventAggregator{

    private final List<Pair<Pattern, String>> mappingMap = new LinkedList<>();

    public EventAggregatorImpl(final String iniFilename) throws IOException, BackingStoreException {
        final Preferences iniPrefs = new IniPreferences(new Ini(new File(iniFilename)));
        final Preferences mappingNode = iniPrefs.node("MAPPING");
        final String[] mappings = mappingNode.keys();
        for (final String mappingName : mappings) {
            final String mappingValuesUnsplitted = mappingNode.get(mappingName, "");
            //final String[] mappingValues = mappingValuesUnsplitted.split("\\s");
            final String[] mappingValues = new String[]{ mappingValuesUnsplitted.trim()}; // DO NOT COMMIT THIS!
            for (final String mappingValue : mappingValues) {
                mappingMap.add(new ImmutablePair<>(Pattern.compile(mappingValue), mappingName));
            }
        }
    }

    public EventAggregatorImpl() {

    }


    public String aggregate(final String name) {
        for (final Pair<Pattern, String> pair : mappingMap) {
            if (pair.getLeft().matcher(name).matches()) {
                return pair.getValue();
            }
        }
        return name;
    }

    public String aggregateSegmentKey(final String originalKey) {
        if (!isEmpty()) {
            final String[] keyPart = originalKey.split(":");
            final StringBuilder sb = new StringBuilder();
            switch (keyPart.length) {
                case 1: {
                    sb.append(aggregate(keyPart[0])).append(":");
                }
                break;
                case 2: {
                    sb.append(aggregate(keyPart[0])).append(":").append(aggregate(keyPart[1]));
                }
                break;
                default:
                    throw new IllegalArgumentException(String.format("Segment key '%s' must have exactly one or two parts: ", originalKey));
            }
            return sb.toString();
        } else {
            return originalKey;
        }
    }

    private boolean isEmpty() {
        return mappingMap.isEmpty();
    }

}
