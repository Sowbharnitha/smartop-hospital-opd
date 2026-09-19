package util;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Lightweight, zero-dependency JSON utility for Core Java.
 * Handles serialization of Maps, Lists, Primitives, and POJOs,
 * as well as recursive parsing of JSON strings into Maps and Lists.
 */
public class JsonUtil {

    // ==========================================
    // SERIALIZATION (Object -> JSON String)
    // ==========================================

    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Character) {
            return "\"" + escape(obj.toString()) + "\"";
        }
        if (obj instanceof java.util.Date) {
            return "\"" + obj.toString() + "\"";
        }
        if (obj instanceof java.time.temporal.Temporal) {
            return "\"" + obj.toString() + "\"";
        }
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Iterable<?>) {
            Iterable<?> iter = (Iterable<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : iter) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(obj);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(java.lang.reflect.Array.get(obj, i)));
            }
            sb.append("]");
            return sb.toString();
        }

        // POJO serialization via getter methods
        Map<String, Object> map = new LinkedHashMap<>();
        for (Method method : obj.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && !method.getName().equals("getClass")) {
                String name = method.getName();
                String propName = null;
                if (name.startsWith("get") && name.length() > 3) {
                    propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                } else if (name.startsWith("is") && name.length() > 2) {
                    propName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                }
                if (propName != null) {
                    try {
                        Object val = method.invoke(obj);
                        map.put(propName, val);
                    } catch (Exception ignored) {}
                }
            }
        }
        return toJson(map);
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
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
        return sb.toString();
    }

    // ==========================================
    // PARSING (JSON String -> Object / Map / List)
    // ==========================================

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object parsed = parse(json);
        if (parsed instanceof Map) {
            return (Map<String, Object>) parsed;
        }
        return new LinkedHashMap<>();
    }

    public static Object parse(String json) {
        if (json == null) return null;
        json = json.trim();
        if (json.isEmpty()) return null;
        Parser parser = new Parser(json);
        return parser.parseValue();
    }

    private static class Parser {
        private final String src;
        private int idx = 0;

        Parser(String src) {
            this.src = src;
        }

        private void skipWhitespace() {
            while (idx < src.length() && Character.isWhitespace(src.charAt(idx))) {
                idx++;
            }
        }

        private char peek() {
            skipWhitespace();
            if (idx >= src.length()) return '\0';
            return src.charAt(idx);
        }

        private char next() {
            skipWhitespace();
            if (idx >= src.length()) return '\0';
            return src.charAt(idx++);
        }

        Object parseValue() {
            char c = peek();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            throw new IllegalArgumentException("Unexpected character at pos " + idx + ": " + c);
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            next(); // consume '{'
            while (true) {
                skipWhitespace();
                char c = peek();
                if (c == '}') {
                    next();
                    break;
                }
                if (c != '"') {
                    throw new IllegalArgumentException("Expected string key in object at pos " + idx);
                }
                String key = parseString();
                skipWhitespace();
                if (next() != ':') {
                    throw new IllegalArgumentException("Expected ':' after key at pos " + idx);
                }
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                c = peek();
                if (c == ',') {
                    next();
                } else if (c == '}') {
                    next();
                    break;
                } else {
                    throw new IllegalArgumentException("Expected ',' or '}' in object at pos " + idx);
                }
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            next(); // consume '['
            while (true) {
                skipWhitespace();
                char c = peek();
                if (c == ']') {
                    next();
                    break;
                }
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                c = peek();
                if (c == ',') {
                    next();
                } else if (c == ']') {
                    next();
                    break;
                } else {
                    throw new IllegalArgumentException("Expected ',' or ']' in array at pos " + idx);
                }
            }
            return list;
        }

        String parseString() {
            next(); // consume opening '"'
            StringBuilder sb = new StringBuilder();
            while (idx < src.length()) {
                char c = src.charAt(idx++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (idx >= src.length()) break;
                    char esc = src.charAt(idx++);
                    switch (esc) {
                        case '"':  sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'u':
                            if (idx + 4 <= src.length()) {
                                String hex = src.substring(idx, idx + 4);
                                sb.append((char) Integer.parseInt(hex, 16));
                                idx += 4;
                            }
                            break;
                        default:
                            sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean parseBoolean() {
            if (src.startsWith("true", idx)) {
                idx += 4;
                return Boolean.TRUE;
            } else if (src.startsWith("false", idx)) {
                idx += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean literal at pos " + idx);
        }

        Object parseNull() {
            if (src.startsWith("null", idx)) {
                idx += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid null literal at pos " + idx);
        }

        Number parseNumber() {
            int start = idx;
            if (src.charAt(idx) == '-') idx++;
            boolean isFloating = false;
            while (idx < src.length()) {
                char c = src.charAt(idx);
                if (Character.isDigit(c)) {
                    idx++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    isFloating = true;
                    idx++;
                } else {
                    break;
                }
            }
            String numStr = src.substring(start, idx);
            if (isFloating) {
                return Double.parseDouble(numStr);
            }
            long val = Long.parseLong(numStr);
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                return (int) val;
            }
            return val;
        }
    }

    // ==========================================
    // CONVENIENCE MAP EXTRACTORS
    // ==========================================

    public static String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : defaultVal;
    }

    public static String getString(Map<String, Object> map, String key) {
        return getString(map, key, "");
    }

    public static int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString().trim()); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    public static int getInt(Map<String, Object> map, String key) {
        return getInt(map, key, 0);
    }
}
