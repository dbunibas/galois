package galois.llm.query.ollama;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import galois.llm.query.IQueryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.SpeedyConstants;
import speedy.model.database.*;
import speedy.model.database.mainmemory.datasource.IntegerOIDGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OllamaMistralQAQueryExecutor implements IQueryExecutor {
    private static final Logger logger = LoggerFactory.getLogger(OllamaMistralQAQueryExecutor.class);

    private final Chain<String, String> chain;

    public OllamaMistralQAQueryExecutor() {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl("http://127.0.0.1:11434")
                .modelName("mistral")
                .temperature(0.0)
                .build();

        ChatMemory memory = new MessageWindowChatMemory.Builder().maxMessages(15).build();
        memory.add(getRulesMessage());
        getFewShots().forEach(memory::add);

        chain = ConversationalChain.builder().chatLanguageModel(model).chatMemory(memory).build();
    }

    @Override
    public List<Tuple> execute(String query, ITable table) {
        String response = chain.execute(query);

        String answers = response
                .replaceAll("(reasoning:).*", "")
                .replaceAll("answer:", "")
                .trim();

        return Arrays.stream(answers.split(","))
                .map(a -> toTuple(a, table))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Tuple toTuple(String answer, ITable table) {
        // TODO: Implement

        TupleOID mockOID = new TupleOID(IntegerOIDGenerator.getNextOID());
        Tuple tuple = new Tuple(mockOID);
        Cell oidCell = new Cell(
                mockOID,
                new AttributeRef(table.getName(), SpeedyConstants.OID),
                new ConstantValue(mockOID)
        );
        tuple.addCell(oidCell);

        List<Attribute> attributes = table.getAttributes().stream()
                .filter(a -> !a.getName().equals("oid"))
                .toList();
        String[] cells = answer.trim().split("\\|");
        logger.info("cells: {}", (Object) cells);
        if (cells.length != attributes.size()) {
            logger.error("Cells length is inconsistent! Cells {} - Attributes {}", cells.length, attributes.size());
            throw new RuntimeException("Cells length is inconsistent!");
        }

        for (int i = 0; i < cells.length; i++) {
            String cellValue = cells[i];
            Attribute attribute = attributes.get(i);
            Cell currentCell = new Cell(
                    mockOID,
                    new AttributeRef(table.getName(), attribute.getName()),
                    new ConstantValue(cellValue)
            );
            tuple.addCell(currentCell);
        }

        return tuple;
    }

    private ChatMessage getRulesMessage() {
        return new SystemMessage("""
                You are a highly intelligent question answering bot.
                You are in charge of giving concise and truthful answers to some user questions.
                If I ask you a question that is rooted in truth, you will give the answer.
                If I ask you a question that is nonsense, trickery, or has no clear answer, you will respond with 'Unknown'
                Each answer should have the following structure
                                
                answer: containing a string with a concise and truthful answer
                reasoning: containing a string explaining why the given answer was selected
                """);
    }

    private List<ChatMessage> getFewShots() {
        List<QAPair> qsWithStructure = List.of(
                new QAPair("What is the capital of France?", toStructuredAnswer("Paris", "Paris is the capital of France")),
                new QAPair("What is the capital of Italy?", toStructuredAnswer("Rome", "Rome is the capital of Italy")),
                new QAPair("Who was the italian prime minister in 2020?", toStructuredAnswer("Giuseppe Conte", "Giuseppe Conte served as prime minister in two terms: from June 1, 2018 to Feb. 13, 2021")),
                new QAPair("How many squigs are in a bonk?", toStructuredAnswer("Unknown", "The question is nonsense")),
                new QAPair("Which party was founded by Gramsci?", toStructuredAnswer("Comunista", "Antonio Gramsci was one of the founders of the Partito Comunista Italiano in 1921")),
                new QAPair("What is the population of Italy?", toStructuredAnswer("~ 60 million", "The population of Italy is estimated to be around 60 million people")),
                new QAPair("List some coaches of the Italian national soccer team. For each of them return name|sex", toStructuredAnswer("Cesare Prandelli|male, Antonio Conte|male, Gian Piero Ventura|male, Luigi Di Biagio|male, Roberto Mancini|male, Luciano Spalletti|male", "Those are the last six coaches of the Italian national soccer team")),
                new QAPair("List some italian regions. For each of them return name|population", toStructuredAnswer("Lombardia|9.976.509, Lazio|5.720.536, Campania|5.609.536, Veneto|4.849.553, Sicilia|4.814.016, Emilia-Romagna|4.437.578, Piemonte|4.251.351, Puglia|3.907.683, Toscana|3.661.981, Calabria|1.846.610, Sardegna|1.578.146, Liguria|1.507.636, Marche|1.484.298, Abruzzo|1.272.627, Friuli Venezia Giulia|1.194.248, Trentino-Alto Adige|1.077.143, Umbria|856.407, Basilicata|537.577, Molise|290.636, Valle d'Aosta|123.130", "Those are some italian regions with respective population"))
        );
        return qsWithStructure.stream()
                .map(qa -> List.of(new UserMessage(qa.question), new AiMessage(qa.answer)))
                .flatMap(List::stream)
                .toList();
    }

    private String toStructuredAnswer(String answer, String reasoning) {
        StringBuilder builder = new StringBuilder();
        builder.append("answer: ").append(answer).append('\n');
        builder.append("reasoning: ").append(reasoning).append('\n');
        return builder.toString();
    }

    private record QAPair(String question, String answer) {
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Q: ").append(question).append("\n");
            builder.append("A: ").append(answer).append("\n");
            return builder.toString();
        }
    }
}
