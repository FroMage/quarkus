package io.quarkus.rest.runtime.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
@SuppressWarnings(value = "rawtypes")
public class MediaTypeHelper {
    public static final MediaTypeComparator COMPARATOR = new MediaTypeComparator();

    public static float getQ(MediaType type) {
        float rtn = getQWithParamInfo(type);
        if (rtn == 2.0F)
            return 1.0F;
        return rtn;
    }

    public static float getQWithParamInfo(MediaType type) {
        if (type.getParameters() != null) {
            String val = type.getParameters().get("q");
            try {
                if (val != null) {
                    float rtn = Float.valueOf(val);
                    if (rtn > 1.0F)
                        throw new WebApplicationException("Media type q greated than 1" + type.toString(),
                                Response.Status.BAD_REQUEST);
                    return rtn;
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Media type q value must be a float" + type, e);
            }
        }
        return 2.0f;
    }

    /**
     * subtypes like application/*+xml
     *
     * @param subtype subtype
     * @return true if subtype is composite
     */
    public static boolean isCompositeWildcardSubtype(String subtype) {
        return subtype.startsWith("*+");
    }

    /**
     * subtypes like application/*+xml
     *
     * @param subtype subtype
     * @return true if subtype is wildcard composite
     */
    public static boolean isWildcardCompositeSubtype(String subtype) {
        return subtype.endsWith("+*");
    }

    public static boolean isComposite(String subtype) {
        return (isCompositeWildcardSubtype(subtype) || isWildcardCompositeSubtype(subtype));
    }

    private static class MediaTypeComparator implements Comparator<MediaType>, Serializable {

        private static final long serialVersionUID = -5828700121582498092L;

        public int compare(MediaType mediaType2, MediaType mediaType) {
            float q = getQWithParamInfo(mediaType);
            boolean wasQ = q != 2.0f;
            if (q == 2.0f)
                q = 1.0f;

            float q2 = getQWithParamInfo(mediaType2);
            boolean wasQ2 = q2 != 2.0f;
            if (q2 == 2.0f)
                q2 = 1.0f;

            if (q < q2)
                return -1;
            if (q > q2)
                return 1;

            if (mediaType.isWildcardType() && !mediaType2.isWildcardType())
                return -1;
            if (!mediaType.isWildcardType() && mediaType2.isWildcardType())
                return 1;
            if (mediaType.isWildcardSubtype() && !mediaType2.isWildcardSubtype())
                return -1;
            if (!mediaType.isWildcardSubtype() && mediaType2.isWildcardSubtype())
                return 1;
            if (isComposite(mediaType.getSubtype()) && !isComposite(mediaType2.getSubtype()))
                return -1;
            if (!isComposite(mediaType.getSubtype()) && isComposite(mediaType2.getSubtype()))
                return 1;
            if (isCompositeWildcardSubtype(mediaType.getSubtype()) && !isCompositeWildcardSubtype(mediaType2.getSubtype()))
                return -1;
            if (!isCompositeWildcardSubtype(mediaType.getSubtype()) && isCompositeWildcardSubtype(mediaType2.getSubtype()))
                return 1;
            if (isWildcardCompositeSubtype(mediaType.getSubtype()) && !isWildcardCompositeSubtype(mediaType2.getSubtype()))
                return -1;
            if (!isWildcardCompositeSubtype(mediaType.getSubtype()) && isWildcardCompositeSubtype(mediaType2.getSubtype()))
                return 1;

            int numNonQ = 0;
            if (mediaType.getParameters() != null) {
                numNonQ = mediaType.getParameters().size();
                if (wasQ)
                    numNonQ--;
            }

            int numNonQ2 = 0;
            if (mediaType2.getParameters() != null) {
                numNonQ2 = mediaType2.getParameters().size();
                if (wasQ2)
                    numNonQ2--;
            }

            if (numNonQ < numNonQ2)
                return -1;
            if (numNonQ > numNonQ2)
                return 1;

            return 0;
        }
    }

    public static int compareWeight(MediaType one, MediaType two) {
        return COMPARATOR.compare(one, two);
    }

    public static boolean sameWeight(MediaType one, MediaType two) {
        return COMPARATOR.compare(one, two) == 0;
    }

    public static void sortByWeight(List<MediaType> types) {
        if (types == null || types.size() <= 1)
            return;
        types.sort(COMPARATOR);
    }

    public static MediaType getBestMatch(List<MediaType> desired, List<MediaType> provided) {
        sortByWeight(desired);
        sortByWeight(provided);
        return getBestMatchNoSort(desired, provided);
    }

    public static MediaType getBestMatchNoSort(List<MediaType> desired, List<MediaType> provided) {
        boolean emptyDesired = desired == null || desired.size() == 0;
        boolean emptyProvided = provided == null || provided.size() == 0;

        if (emptyDesired && emptyProvided)
            return null;
        if (emptyDesired)
            return provided.get(0);
        if (emptyProvided)
            return desired.get(0);

        for (MediaType desire : desired) {
            for (MediaType provide : provided) {
                if (provide.isCompatible(desire))
                    return provide;
            }
        }
        return null;
    }

    public static MediaType getBestConcreteMatch(List<MediaType> desired, List<MediaType> provided) {
        sortByWeight(desired);
        sortByWeight(provided);
        boolean emptyDesired = desired == null || desired.size() == 0;
        boolean emptyProvided = provided == null || provided.size() == 0;

        if (emptyDesired && emptyProvided)
            return null;
        if (emptyDesired)
            return provided.get(0);
        if (emptyProvided)
            return desired.get(0);

        for (MediaType desire : desired) {
            for (MediaType provide : provided) {
                if (provide.isCompatible(desire)) {
                    if (provide.isWildcardType()) {
                        return desire;
                    } else {
                        return provide;
                    }
                }
            }
        }
        return null;
    }

    public static List<MediaType> parseHeader(String header) {
        ArrayList<MediaType> types = new ArrayList<MediaType>();
        String[] medias = header.split(",");
        for (String media : medias) {
            types.add(MediaType.valueOf(media.trim()));
        }
        return types;
    }

    public static boolean equivalent(MediaType m1, MediaType m2) {
        if (m1 == m2)
            return true;

        if (!m1.getType().equals(m2.getType()))
            return false;
        if (!m1.getSubtype().equals(m2.getSubtype()))
            return false;

        return equivalentParams(m1, m2);
    }

    public static boolean equivalentParams(MediaType m1, MediaType m2) {
        Map<String, String> params1 = m1.getParameters();
        Map<String, String> params2 = m2.getParameters();

        if (params1 == params2)
            return true;
        if (params1 == null || params2 == null)
            return false;
        if (params1.size() == 0 && params2.size() == 0)
            return true;
        int numParams1 = params1.size();
        if (params1.containsKey("q"))
            numParams1--;
        int numParams2 = params2.size();
        if (params2.containsKey("q"))
            numParams2--;

        if (numParams1 != numParams2)
            return false;
        if (numParams1 == 0)
            return true;

        for (Map.Entry<String, String> entry : params1.entrySet()) {
            String key = entry.getKey();
            if (key.equals("q"))
                continue;
            String value = entry.getValue();
            String value2 = params2.get(key);
            if (value == value2)
                continue; // both null
            if (value == null || value2 == null)
                return false;
            if (value.equals(value2) == false)
                return false;
        }
        return true;
    }

    public static boolean isTextLike(MediaType mediaType) {
        return "text".equalsIgnoreCase(mediaType.getType())
                || ("application".equalsIgnoreCase(mediaType.getType())
                        && mediaType.getSubtype().toLowerCase().startsWith("xml"));
    }

    public static boolean isBlacklisted(MediaType mediaType) {
        return "application".equals(mediaType.getType()) && "signed-exchange".equals(mediaType.getSubtype());
    }

    public static boolean isUnsupportedWildcardSubtype(MediaType mediaType) {
        if (mediaType.isWildcardSubtype()) {
            return !mediaType.isWildcardType() && !"application".equals(mediaType.getType());
        }
        return false;
    }
}
