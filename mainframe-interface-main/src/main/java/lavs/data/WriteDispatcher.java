package lavs.data;

import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;

// AI-assisted: the composition root supplies durable writes without a data-to-logic dependency.
@FunctionalInterface
public interface WriteDispatcher {
    MessageResponse send(MessageRequest request);
}
