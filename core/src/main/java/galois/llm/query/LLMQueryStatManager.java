package galois.llm.query;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LLMQueryStatManager {
    
    private static LLMQueryStatManager singleton = new LLMQueryStatManager();
    
    private int LLMRequest;
    private double LLMTokensInput;
    private double LLMTokensOutput;
    private long timeMs;
    
    private LLMQueryStatManager() {
        this.LLMRequest = 0;
        this.LLMTokensInput = 0;
        this.LLMTokensOutput = 0;
        this.timeMs = 0;
    }
    
    public static LLMQueryStatManager getInstance() {
        return singleton;
    }

    public int getLLMRequest() {
        return LLMRequest;
    }

    public double getLLMTokensInput() {
        return LLMTokensInput;
    }

    public double getLLMTokensOutput() {
        return LLMTokensOutput;
    }

    public long getTimeMs() {
        return timeMs;
    }
    
    public void resetStats() {
        this.LLMRequest = 0;
        this.LLMTokensInput = 0;
        this.LLMTokensOutput = 0;
        this.timeMs = 0;
    }
    
    public void updateLLMRequest(int increment) {
        this.LLMRequest += increment;
    }
    
    public void updateLLMTokensInput(double increment) {
        this.LLMTokensInput += increment;
    }
    
    public void updateLLMTokensOutput(double increment) {
        this.LLMTokensOutput += increment;
    }
    
    public void updateTimeMs(long increment) {
        this.timeMs += increment;
    }
    
    
}
