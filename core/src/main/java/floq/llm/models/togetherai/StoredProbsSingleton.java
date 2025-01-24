package floq.llm.models.togetherai;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class StoredProbsSingleton {

    private static StoredProbsSingleton singleton = new StoredProbsSingleton();
    private Map<String, Logprobs> cachedProbs = new HashMap<>();
    
    public static StoredProbsSingleton getInstance() {
        return singleton;
    }

    private StoredProbsSingleton() {

    }
    
    public void putProbs(String prompt, Logprobs probs) {
        this.cachedProbs.put(prompt, probs);
    }
    
    public Logprobs getLogprobs(String prompt) {
        return this.cachedProbs.get(prompt);
    }

}
