package ru.usharik.k8s.client.logger;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

public class TextAreaAppender extends WriterAppender {

    private static volatile TextArea textArea = null;

    public static void setTextArea(TextArea textArea) {
        TextAreaAppender.textArea = textArea;
    }

    @Override
    public void append(final LoggingEvent loggingEvent) {
        String message = this.layout.format(loggingEvent);
        try {
            Platform.runLater(() -> {
                try {
                    if (textArea != null) {
                        textArea.appendText(message);
                    }
                } catch (Exception t) {
                    System.out.printf("Unable to append log to text area: %s", t.getMessage());
                }
            });
        } catch (IllegalStateException e) {
            // ignore case when the platform hasn't yet been initialized
        }
    }
}
