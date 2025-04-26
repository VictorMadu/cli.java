package io.github.victormadu.display;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.victormadu.display.annotation.Column;

class DisplayTest {
    private Display display;

    @BeforeEach
    void setUp() {
        display = new Display();
    }

    static class TestData {
        @Column("Name")
        private String name;

        @Column("Age")
        private int age;

        public TestData(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    @Test
    void should_NotThrowException_When_InputIsNull() {
        assertDoesNotThrow(() -> display.table(null));
    }

    @Test
    void should_DisplayTable_When_SingleObjectProvided() {
        TestData data = new TestData("John", 30);
        assertDoesNotThrow(() -> display.table(data));
    }

    @Test
    void should_DisplayTable_When_ListProvided() {
        List<TestData> data = Arrays.asList(
            new TestData("John", 30),
            new TestData("Jane", 25)
        );
        assertDoesNotThrow(() -> display.table(data, TestData.class));
    }

    @Test
    void should_DisplayTable_When_ArrayProvided() {
        TestData[] data = {
            new TestData("John", 30),
            new TestData("Jane", 25)
        };
        assertDoesNotThrow(() -> display.table(data, TestData.class));
    }

    @Test
    void should_DisplayTable_When_PrimitiveArrayProvided() {
        int[] numbers = {1, 2, 3, 4, 5};
        assertDoesNotThrow(() -> display.table(numbers));
    }

    @Test
    void should_NotThrowException_When_EmptyListProvided() {
        List<TestData> emptyList = new ArrayList<>();
        assertDoesNotThrow(() -> display.table(emptyList));
    }

    @Test
    void should_NotThrowException_When_EmptyListAndClassProvided() {
        List<TestData> emptyList = new ArrayList<>();
        assertDoesNotThrow(() -> display.table(emptyList, TestData.class));
    }

    @Test
    void should_DisplayException_When_ThrowableProvided() {
        IllegalArgumentException exception = new IllegalArgumentException("There is an error");
        assertDoesNotThrow(() -> display.table(exception));
    }

    @Test
    void should_DisplayTable_When_GenericReturnTypeIsList() throws Throwable {
        class Data {
            @Column("Name")
            private String name;
        }

        class TestDataWithList {
            public List<Data> rows(List<Data> rows) {
                return rows;
            }
        }

        TestDataWithList data = new TestDataWithList();
        Object rows = data.rows(new ArrayList<>());

        Type genericReturnType = TestDataWithList.class.getMethods()[0].getGenericReturnType();
        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
        Class<?> typeArgument = (Class<?>) parameterizedType.getActualTypeArguments()[0];

        assertDoesNotThrow(() -> display.table(rows, typeArgument));
    }

    @Test
    void should_DisplayTable_When_ExceptionProvided() {
        Exception exception = new Exception("Test exception");
        assertDoesNotThrow(() -> display.table(exception));
    }

    @Test
    void should_HandleOptionalFieldCorrectly_When_DisplayTableCalled() {
        class OptionalData {
            @Column("Value")
            private Optional<String> value = Optional.of("test");
        }

        OptionalData optionalData = new OptionalData();
        assertDoesNotThrow(() -> display.table(optionalData));
    }
}
