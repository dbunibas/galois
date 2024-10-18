package floq.llm.query;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class LLMQueryStatManager {
    
    private static LLMQueryStatManager singleton = new LLMQueryStatManager();

    private int LLMRequest;
    private double LLMTokensInput;
    private double LLMTokensOutput;
    private long timeMs;

    private int baseLLMRequest;
    private double baseLLMTokensInput;
    private double baseLLMTokensOutput;
    private long basetimeMs;
    
    private LLMQueryStatManager() {
        resetStats();
    }
    
    public static LLMQueryStatManager getInstance() {
        return singleton;
    }
    
    public void resetStats() {
        this.LLMRequest = 0;
        this.LLMTokensInput = 0;
        this.LLMTokensOutput = 0;
        this.timeMs = 0;
        this.baseLLMRequest = 0;
        this.baseLLMTokensInput = 0;
        this.baseLLMTokensOutput = 0;
        this.basetimeMs = 0;
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


    public void updateBaseLLMRequest(int increment) {
        this.baseLLMRequest += increment;
    }

    public void updateBaseLLMTokensInput(double increment) {
        this.baseLLMTokensInput += increment;
    }

    public void updateBaseLLMTokensOutput(double increment) {
        this.baseLLMTokensOutput += increment;
    }

    public void updateBaseTimeMs(long increment) {
        this.basetimeMs += increment;
    }



}
