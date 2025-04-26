package io.github.victormadu.command;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.victormadu.command.annotation.Command;
import io.github.victormadu.command.annotation.Param;

class CommandHandler {
    private final MethodHandle methodHandle;
    private final Method method;
    private final String name;
    private final Map<String, Class<?>> parameterMap = new LinkedHashMap<>();
    
    private Class<?> genericReturnType;

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
            this.methodHandle = MethodHandles.lookup()
                    .findVirtual(
                            method.getDeclaringClass(),
                            method.getName(),
                            MethodType.methodType(method.getReturnType(), method.getParameterTypes()))
                     .bindTo(service);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create command handler", e);
        } 
        this.method = method;
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

        return methodHandle.invokeWithArguments(args);
    }

    public Class<?> getGenericReturnType() {
        if (genericReturnType == null) {
            Type gReturnType = method.getGenericReturnType();
            ParameterizedType parameterizedType = (ParameterizedType) gReturnType;
            genericReturnType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        }
        
        return genericReturnType;
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