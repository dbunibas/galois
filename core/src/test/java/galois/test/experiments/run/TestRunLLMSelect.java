package galois.test.experiments.run;

import galois.test.experiments.Experiment;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.json.parser.ExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
public class TestRunLLMSelect {
    @Test
    public void testContinentsLlama3Experiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/continents/continents-llama3-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testContinentsMistralExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/continents/continents-mistral-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testContinentsLlamaCppExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/continents/continents-llama_cpp-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testContinentsOutlinesExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/continents/continents-outlines-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testActorExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/dummyActors/actors-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testDisneyMovieExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/disney-movies/movies-outlines-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testInternationalFootballLlama3Experiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/football-llama3-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testInternationalFootballMistralExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/football-mistral-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testInternationalFootballLlamcppExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/football-llamacpp-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testInternationalFootballOutlinesExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/football-outlines-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testPremierLeagueLLama3Experiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/premier-league-llama3-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testPremierLeagueMistralExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/premier-league-mistral-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testPremierLeagueLlamaCppExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/premier-league-llamacpp-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testPremierLeagueOutlinesExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/premier-league-outlines-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testOlympicsMistralExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/olympics-tokyo/olympics-mistral-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testOlympicsExperimentLlama3() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/olympics-tokyo/olympics-llama3-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testOlympicsExperimentLlamacpp() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/olympics-tokyo/olympics-llama_cpp-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testOlympicsExperimentOutlines() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/olympics-tokyo/olympics-outlines-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testPresidentsExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/presidents-usa/presidents-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testPresidentsExperimentLlama3() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/presidents-usa/presidents-llama3-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }
}
