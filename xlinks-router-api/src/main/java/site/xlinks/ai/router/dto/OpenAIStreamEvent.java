package site.xlinks.ai.router.dto;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Structured SSE event forwarded from upstream.
 */
@Value
@Builder
public class OpenAIStreamEvent {

    String event;

    String id;

    Long retry;

    @Singular("comment")
    List<String> comments;

    @Singular("dataLine")
    List<String> dataLines;

    public boolean hasData() {
        return dataLines != null && !dataLines.isEmpty();
    }

    public String joinedData() {
        if (!hasData()) {
            return "";
        }
        return String.join("\n", dataLines);
    }

    public boolean isDoneSignal() {
        return "[DONE]".equals(joinedData());
    }
}
