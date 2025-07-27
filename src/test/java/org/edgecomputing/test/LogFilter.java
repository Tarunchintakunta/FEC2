package org.edgecomputing.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * A utility class to filter unwanted log messages from CloudSim Plus
 */
public class LogFilter extends ByteArrayOutputStream {
    private final PrintStream original;
    
    public LogFilter(PrintStream original) {
        this.original = original;
    }
    
    @Override
    public void flush() throws IOException {
        String content = toString();
        
        // Filter out CloudSim warnings and verbose messages but keep important test output
        if (!shouldFilter(content)) {
            original.print(content);
        }
        
        reset();
    }
    
    /**
     * Determine if a log message should be filtered out
     * @param message The log message to check
     * @return true if the message should be filtered (not displayed)
     */
    private boolean shouldFilter(String message) {
        // Always show energy-related messages regardless of other content
        if (message.contains("energy consumed") || 
            message.contains("Energy consumption") || 
            message.contains("Average energy")) {
            return false;
        }
        
        // Filter out common noisy messages
        return message.contains("Cannot send events before simulation starts") ||
               message.contains("WARN") ||
               message.startsWith("WARN") ||
               message.contains("Checking new events") ||
               message.contains("Waiting more events");
    }
}
