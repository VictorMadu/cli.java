package io.github.victormadu.command;

import java.util.Map;

public class CommandRunner {
    private final CommandHandler handler;
    private final Map<String, String> params;
    
    CommandRunner(CommandHandler handler, Map<String, String> params) {
        this.handler = handler;
        this.params = params;
    }

    public Object run() throws Throwable {
        return handler.execute(params);
    }

    public Class<?> getGenericClassOfReturnType() {
        return handler.getGenericReturnType();
    }
}
