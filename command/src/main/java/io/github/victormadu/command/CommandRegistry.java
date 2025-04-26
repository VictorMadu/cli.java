package io.github.victormadu.command;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.github.victormadu.command.annotation.Command;

public class CommandRegistry {
    private final Map<String, CommandHandler> handlers = new HashMap<>();

    public CommandRegistry(Object... services) {
        for (Object service : services) {
            registerService(service);
        }
    }

    public final void registerService(Object service) {
        for (Method method : service.getClass().getDeclaredMethods()) {            
           
            if (method.isAnnotationPresent(Command.class)) {
                CommandHandler handler = new CommandHandler(service, method);
                if (handlers.containsKey(handler.name())) {
                    throw new IllegalArgumentException("Duplicate command name: " + handler.name());
                }
                handlers.put(handler.name(), handler);
            }
        }
    }

    public CommandRunner getRunner(String command) throws Throwable {
        String trimmedCommand = command.trim();
        String commandName = extractCommandName(trimmedCommand);
        Map<String, String> params = parseParameters(trimmedCommand.substring(commandName.length()).trim());
        CommandHandler handler = handlers.get(commandName);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }

        return new CommandRunner(handler, params);
    }

    private String extractCommandName(String command) {
        int spaceIndex = command.indexOf(' ');
        if (spaceIndex == -1) return command;
        if (spaceIndex == 0) throw new IllegalArgumentException("Invalid command format");
        return command.substring(0, spaceIndex);
    }

    private Map<String, String> parseParameters(String paramString) {
        if (paramString.isEmpty()) return new HashMap<>();

        Map<String, String> params = new HashMap<>();
        int currentPos = 0;
        int length = paramString.length();

        while (currentPos < length) {
            // Skip leading spaces
            while (currentPos < length && Character.isWhitespace(paramString.charAt(currentPos))) {
                currentPos++;
            }
            if (currentPos >= length) break;

            // Find parameter name
            int equalsPos = paramString.indexOf('=', currentPos);
            if (equalsPos == -1) throw new IllegalArgumentException("Invalid parameter format: missing '='");
            
            String paramName = paramString.substring(currentPos, equalsPos).trim();
            if (paramName.isEmpty()) throw new IllegalArgumentException("Empty parameter name");

            currentPos = equalsPos + 1;
            if (currentPos >= length) throw new IllegalArgumentException("Missing parameter value");

            // Skip leading spaces
            while (currentPos < length && Character.isWhitespace(paramString.charAt(currentPos))) {
                currentPos++;
            }
            if (currentPos >= length) break;

            // Parse parameter value
            if (paramString.charAt(currentPos) == '"') {
                // Handle quoted value
                
                StringBuilder param = new StringBuilder();
                
                OUTER:
                while (++currentPos < length) {
                    char ch = paramString.charAt(currentPos);

                    switch (ch) {
                        case '\\':
                            param.append(paramString.charAt(++currentPos));
                            break;
                        case '"':
                            ++currentPos;
                            break OUTER;
                        default:
                            param.append(paramString.charAt(currentPos));
                            break;
                    }
                }

                params.put(paramName, param.toString());
            } else {
                // Handle unquoted value  
                int valueStart = currentPos;
            
                while (currentPos < length && !Character.isWhitespace(paramString.charAt(currentPos))) {
                    currentPos++;
                }

                String paramValue = paramString.substring(valueStart, currentPos);
                params.put(paramName, paramValue);
            }
        }

        return params;
    }
}