package potato;

import java.util.ArrayList;

public class Logger {
    public static ArrayList<String> logs = new ArrayList<>();
    private final String className;
    private String prefix = "";

    public Logger(String className) {
        this.className = "[" + className + "] ";
    }

    public void addPrefix(String prefix) {
        this.prefix += "[" + prefix + "] ";
    }

    private void logInternal(String msg) {
        String logMessage = this.className + this.prefix + msg;
        System.out.println(logMessage);
        logs.add(logMessage);
    }

    public void Log(String msg) {
        logInternal(msg);
    }

    public void Error(String msg) {
        logInternal("[ERROR] " + msg);
    }

    public void Error(Exception err) {
        StringBuilder stackTraceBuilder = new StringBuilder();
        stackTraceBuilder.append("[ERROR]\n");
        stackTraceBuilder.append(err.getMessage());
        stackTraceBuilder.append("\n");
        for (StackTraceElement element : err.getStackTrace()) {
            stackTraceBuilder.append(element.toString()).append("\n");
        }
        String stackTraceString = stackTraceBuilder.toString();
        logInternal(stackTraceString);
    }
}