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
    public void testDummyExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/dummy-continents/dummy-experiment.json");
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
    public void testInternationalFootballExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/football-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testPremierLeagueExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/football/premier-league-experiment.json");
        ExperimentResults results = experiment.execute();
        log.info("{}", results);
    }

    @Test
    public void testOlympicsExperiment() throws IOException {
        Experiment experiment = ExperimentParser.loadAndParseJSON("/olympics-tokyo/olympics-experiment.json");
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
