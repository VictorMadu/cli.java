package io.github.victormadu.command;

import java.util.ArrayList;
import java.util.List;

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
}