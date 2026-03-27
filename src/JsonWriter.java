import java.util.Collection;
import java.util.Map;

/**
 * Minimal JSON serializer — zero external dependencies.
 * Converts Map/List/primitive structures to valid JSON strings.
 * Used by AgentAPI and CLI --json mode for machine-readable output.
 */
public final class JsonWriter {
    private JsonWriter() {}

    public static String toJson(Object obj) {
        return toJson(obj, 0, true);
    }

    public static String toJsonCompact(Object obj) {
        return toJson(obj, 0, false);
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object obj, int indent, boolean pretty) {
        if (obj == null) return "null";
        if (obj instanceof Boolean) return obj.toString();
        if (obj instanceof Number) return formatNumber((Number) obj);
        if (obj instanceof String) return escapeString((String) obj);
        if (obj instanceof Map) return mapToJson((Map<String, Object>) obj, indent, pretty);
        if (obj instanceof Collection) return collectionToJson((Collection<?>) obj, indent, pretty);
        return escapeString(obj.toString());
    }

    private static String mapToJson(Map<String, Object> map, int indent, boolean pretty) {
        if (map.isEmpty()) return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (pretty) sb.append('\n');

        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0) {
                sb.append(',');
                if (pretty) sb.append('\n');
            }
            if (pretty) sb.append(spaces(indent + 2));
            sb.append(escapeString(entry.getKey()));
            sb.append(pretty ? ": " : ":");
            sb.append(toJson(entry.getValue(), indent + 2, pretty));
            i++;
        }

        if (pretty) {
            sb.append('\n');
            sb.append(spaces(indent));
        }
        sb.append('}');
        return sb.toString();
    }

    private static String collectionToJson(Collection<?> coll, int indent, boolean pretty) {
        if (coll.isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        if (pretty) sb.append('\n');

        int i = 0;
        for (Object item : coll) {
            if (i > 0) {
                sb.append(',');
                if (pretty) sb.append('\n');
            }
            if (pretty) sb.append(spaces(indent + 2));
            sb.append(toJson(item, indent + 2, pretty));
            i++;
        }

        if (pretty) {
            sb.append('\n');
            sb.append(spaces(indent));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escapeString(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String formatNumber(Number n) {
        if (n instanceof Double || n instanceof Float) {
            double d = n.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        return n.toString();
    }

    private static String spaces(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(' ');
        return sb.toString();
    }
}
