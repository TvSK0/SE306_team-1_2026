package lavs.logic;

import lavs.data.DuplicateCheckResult;
import lavs.data.SendResult;
import lavs.data.UploadGateway;
import lavs.model.PendingChange;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

// AI-assisted: test helper generated with Claude (team-approved).
/**
 * A scripted mainframe for sync tests. Answers are used in order; once they run out it
 * accepts every send and finds no duplicates. Records what was sent.
 */
class FakeUploadGateway implements UploadGateway {

    private final Deque<SendResult> sendAnswers = new ArrayDeque<>();
    private final Deque<DuplicateCheckResult> duplicateAnswers = new ArrayDeque<>();
    final List<String> sent = new ArrayList<>();
    final List<Map<String, String>> sentFields = new ArrayList<>();
    int duplicateSearches;
    volatile boolean connected = true;
    volatile boolean offline;

    synchronized FakeUploadGateway answerSend(SendResult... answers) {
        sendAnswers.addAll(List.of(answers));
        return this;
    }

    synchronized FakeUploadGateway answerDuplicate(DuplicateCheckResult... answers) {
        duplicateAnswers.addAll(List.of(answers));
        return this;
    }

    @Override
    public synchronized SendResult send(PendingChange change) {
        if (offline) {
            return SendResult.networkFailure("offline");
        }
        SendResult answer = sendAnswers.isEmpty() ? SendResult.success() : sendAnswers.poll();
        if (answer.getOutcome() == SendResult.Outcome.SUCCESS) {
            sent.add(change.getMessageType());
            sentFields.add(change.getFields());
        }
        return answer;
    }

    @Override
    public synchronized DuplicateCheckResult findExistingCustomers(PendingChange createCustomer) {
        duplicateSearches++;
        return duplicateAnswers.isEmpty() ? DuplicateCheckResult.none() : duplicateAnswers.poll();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
