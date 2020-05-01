package app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ExceptionSet {
    public static Set<String> exceptions = new HashSet<String>(
            Arrays.asList("IETF",
            "RFC",
            "AMS",
            "Montreal",
            "IAB",
            "AQM")
    );
}
