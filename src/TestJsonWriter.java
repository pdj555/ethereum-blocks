import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TestJsonWriter {

    @Test
    void testNullValue() {
        assertEquals("null", JsonWriter.toJsonCompact(null));
    }

    @Test
    void testStringEscaping() {
        assertEquals("\"hello\"", JsonWriter.toJsonCompact("hello"));
        assertEquals("\"he said \\\"hi\\\"\"", JsonWriter.toJsonCompact("he said \"hi\""));
        assertEquals("\"line1\\nline2\"", JsonWriter.toJsonCompact("line1\nline2"));
    }

    @Test
    void testNumbers() {
        assertEquals("42", JsonWriter.toJsonCompact(42));
        assertEquals("3.14", JsonWriter.toJsonCompact(3.14));
        assertEquals("100", JsonWriter.toJsonCompact(100L));
    }

    @Test
    void testBoolean() {
        assertEquals("true", JsonWriter.toJsonCompact(true));
        assertEquals("false", JsonWriter.toJsonCompact(false));
    }

    @Test
    void testEmptyMap() {
        assertEquals("{}", JsonWriter.toJsonCompact(new LinkedHashMap<>()));
    }

    @Test
    void testSimpleMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "test");
        map.put("value", 42);
        String json = JsonWriter.toJsonCompact(map);
        assertEquals("{\"name\":\"test\",\"value\":42}", json);
    }

    @Test
    void testNestedMap() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("x", 1);
        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("nested", inner);
        String json = JsonWriter.toJsonCompact(outer);
        assertEquals("{\"nested\":{\"x\":1}}", json);
    }

    @Test
    void testList() {
        List<Object> list = new ArrayList<>();
        list.add(1);
        list.add("two");
        list.add(3.0);
        String json = JsonWriter.toJsonCompact(list);
        assertEquals("[1,\"two\",3]", json);
    }

    @Test
    void testPrettyPrintContainsNewlines() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");
        String json = JsonWriter.toJson(map);
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("\"key\""));
    }
}
