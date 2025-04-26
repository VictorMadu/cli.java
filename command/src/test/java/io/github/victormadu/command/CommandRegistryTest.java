package io.github.victormadu.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import io.github.victormadu.command.annotation.Command;
import io.github.victormadu.command.annotation.Param;

class CommandRegistryTest {

    @Test
    void should_RegisterServiceAndExecuteCommand_When_ValidCommandsProvided() throws Throwable {
        class Service {
            @Command("hello")
            public void helloCommand() {}

            @Command("hello2")
            public List<String> helloCommand(
                    @Param("param1") String param1,
                    @Param("param2") Integer param2
            ) {
                List<String> result = new ArrayList<>();
                result.add(param1);
                result.add(param2.toString());
                return result;
            }
        }

        // Arrange
        CommandRegistry registry = new CommandRegistry();
        Service service = new Service();

        // Act
        assertDoesNotThrow(() -> registry.registerService(service));

        CommandRunner runner = registry.getRunner("hello2 param1=\"Victor \\\" Madu\" param2=456");
        Object actualResult = runner.run();

        List<String> expectedResult = new ArrayList<>();
        expectedResult.add("Victor \" Madu");
        expectedResult.add("456");

        // Assert
        assertEquals(expectedResult, actualResult);
    }

    static class Data {
        private String name;
        private int age;
    }

    @Test
    void should_DetectGenericClassOfReturnType_ForDifferentCommandOutputs() throws Throwable {

        
        class Service {
            @Command("string")
            public String string() {
                return "hello";
            }

            @Command("stringList")
            public List<String> stringList() {
                List<String> result = new ArrayList<>();
                result.add("hello");
                result.add("world");
                return result;
            }

            @Command("data")
            public Data data() {
                Data data = new Data();
                data.name = "hello";
                data.age = 123;
                return data;
            }

            @Command("dataList")
            public List<Data> dataList() {
                List<Data> result = new ArrayList<>();
                Data data = new Data();
                data.name = "hello";
                data.age = 123;
                result.add(data);
                return result;
            }
        }
        
        CommandRegistry registry = new CommandRegistry();
        registry.registerService(new Service());

        CommandRunner runner;

        runner = registry.getRunner("string");
        assertEquals(Optional.empty(), runner.getGenericClassOfReturnType());

        runner = registry.getRunner("stringList");
        assertEquals(Optional.of(String.class), runner.getGenericClassOfReturnType());

        runner = registry.getRunner("data");
        assertEquals(Optional.empty(), runner.getGenericClassOfReturnType());

        runner = registry.getRunner("dataList");
        assertEquals(Optional.of(Data.class), runner.getGenericClassOfReturnType());
    }
}