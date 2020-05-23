package app;

import java.util.HashSet;
import java.util.Set;

public class UrlSet {
    public static volatile Set<String> ignoredUrls = new HashSet<String>();
    public static volatile Set<String> visitedUrls = new HashSet<String>();
}
