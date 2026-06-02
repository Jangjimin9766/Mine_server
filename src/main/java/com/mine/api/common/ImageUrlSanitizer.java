package com.mine.api.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

public final class ImageUrlSanitizer {

    private static final Set<String> DEFAULT_IMAGE_MARKERS = Set.of(
            "/assets/default-placeholder.png",
            "/assets/default-thumbnail.png");

    private ImageUrlSanitizer() {
    }

    public static String nullIfDefault(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        for (String marker : DEFAULT_IMAGE_MARKERS) {
            if (imageUrl.contains(marker)) {
                return null;
            }
        }
        return normalizeDisplayUrl(imageUrl);
    }

    public static String nullIfDefaultOrDuplicate(String imageUrl, Collection<String> usedImageUrls) {
        String sanitizedUrl = nullIfDefault(imageUrl);
        if (sanitizedUrl == null) {
            return null;
        }
        String dedupeKey = normalizeForDeduplication(sanitizedUrl);
        if (usedImageUrls.contains(dedupeKey)) {
            return null;
        }
        usedImageUrls.add(dedupeKey);
        return sanitizedUrl;
    }

    private static String normalizeDisplayUrl(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String host = uri.getHost();
            if (host == null || !host.equalsIgnoreCase("images.pexels.com")) {
                return imageUrl;
            }
            return new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    "auto=compress&cs=tinysrgb&w=1200",
                    null)
                    .toString();
        } catch (URISyntaxException e) {
            return imageUrl;
        }
    }

    private static String normalizeForDeduplication(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || path == null) {
                return imageUrl;
            }
            return host.toLowerCase(Locale.ROOT) + path;
        } catch (URISyntaxException e) {
            return imageUrl;
        }
    }
}
