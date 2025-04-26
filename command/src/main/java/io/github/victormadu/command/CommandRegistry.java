package io.github.victormadu.command;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.victormadu.command.annotation.Command;
import io.github.victormadu.command.annotation.Param;

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

    public Object execute(String command) throws Throwable {
        String trimmedCommand = command.trim();
        String commandName = extractCommandName(trimmedCommand);
        Map<String, String> params = parseParameters(trimmedCommand.substring(commandName.length()).trim());
        return executeCommand(commandName, params);
    }

    private String extractCommandName(String command) {
        int spaceIndex = command.indexOf(' ');
        if (spaceIndex == -1) return command;
        if (spaceIndex == 0) throw new IllegalArgumentException("Invalid command format");
        return command.substring(0, spaceIndex);
    }

    private Map<String, String> parseParameters(String paramString) {
        if (paramString.isEmpty()) return new HashMap<>();

        Map<String, String> params = new HashMap<String, String>();
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


    private Object executeCommand(String commandName, Map<String, String> params) throws Throwable {
        CommandHandler commandMethod = handlers.get(commandName);
        if (commandMethod == null) {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }
        return commandMethod.execute(params);
    }

    private static class CommandHandler {
        private final MethodHandle method;
        private final String name;
        private final Map<String, Class<?>> parameterMap = new LinkedHashMap<>();
        // private final boolean hasReturnValue;

        public CommandHandler(Object service, Method method) {
            Command commandAnnotation = method.getAnnotation(Command.class);
            if (commandAnnotation == null) {
                throw new IllegalArgumentException("Method is not annotated with @Command");
            }
            
            String commandName = commandAnnotation.name();
            if (commandName == null || commandName.isEmpty()) {
                commandName = commandAnnotation.value();
            }

            if (commandName == null || commandName.isEmpty()) {
                throw new IllegalArgumentException("Command name is not specified");
            }

            this.name = commandName;
            // this.hasReturnValue = method.getReturnType() != void.class;
           
            for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                Param paramAnnotation = parameter.getAnnotation(Param.class);
                if (paramAnnotation == null) {
                    throw new IllegalArgumentException("Parameter is not annotated with @Param");
                }
               
                String paramName = paramAnnotation.name();
                   
                if (paramName == null || paramName.isEmpty()) {
                    paramName = paramAnnotation.value();
                }
                if (paramName == null || paramName.isEmpty()) {
                    throw new IllegalArgumentException("Parameter name is not specified");
                }

                parameterMap.put(paramName, parameter.getType());
            }
            
            try {
                this.method = MethodHandles.lookup()
                        .findVirtual(
                                method.getDeclaringClass(),
                                method.getName(),
                                MethodType.methodType(method.getReturnType(), method.getParameterTypes()))
                         .bindTo(service);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException("Failed to create command handler", e);
            } 
        }

        public Object execute(Map<String, String> params) throws Throwable {
            Object[] args = new Object[parameterMap.size()];
            int i = 0;

            for (Map.Entry<String, Class<?>> entry : parameterMap.entrySet()) {
                String paramName = entry.getKey();
                String paramValue = params.get(paramName);

                if (paramValue == null) {
                    throw new IllegalArgumentException("Missing required parameter: " + paramName);
                }

                args[i++] = convertValue(paramValue, entry.getValue());
            }

            return method.invokeWithArguments(args);
        }

        public String name() {
            return name;
        }

        private Object convertValue(String value, Class<?> type) {
            if (type == String.class) {
                return value;
            } else if (type == Long.class || type == long.class) {
                return Long.valueOf(value);
            } else if (type == Integer.class || type == int.class) {
                return Integer.valueOf(value);
            } else if (type == Boolean.class || type == boolean.class) {
                return Boolean.valueOf(value);
            } else if (type == Instant.class) {
                return Instant.parse(value);
            } else if (type == LocalDate.class) {
                return LocalDate.parse(value);
            }
            throw new IllegalArgumentException("Unsupported parameter type: " + type);
        }
    }
}