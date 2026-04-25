package flashcards;

import java.io.*;

import java.nio.charset.Charset;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

class DualPrintStreamLogger extends PrintStream {

    List<String> consoleLogger;

    //capturing System output to console
    public DualPrintStreamLogger(OutputStream main, List<String> consoleLogger) {
        super(main);
        this.consoleLogger = consoleLogger;
    }

    //Override for String
    @Override
    public void print(String x) {
        super.print(x);
        consoleLogger.add(x);
    }

    //Override print for StringBuilder
    public void print(Object x) {
        super.print(x);
        consoleLogger.add(x.toString());
    }

    //Override for String
    @Override
    public void println(String x) {
        super.println(x);
        consoleLogger.add("\n");
    }

    //Override print for StringBuilder
    @Override
    public void println(Object x) {
        super.println(x);
        consoleLogger.add("\n");
    }

    public List<String> getConsoleLogger() {
        return consoleLogger;
    }

    //capture System input from console
    public void logScannerInput(String sc) {
        consoleLogger.add(sc);
        consoleLogger.add("\n");
    }

}

class Commands {
    String exportFile;
    DeckCreator creator;
    DeckStore store;
    Statistics stats;

    public Commands(DeckCreator creator, Statistics stats, DeckStore store) {
        this.creator = creator;
        this.stats = stats;
        this.store = store;
    }

    void executeImport(String importFile) {

        if (!importFile.isBlank()) {
            creator.setDeck(store.getDeckFromFile(creator, importFile));
        }
    }

    void executeExport() {
        store.storeDeckToFile(creator, stats, exportFile);
    }

    String getExportFile() {
        return exportFile;
    }

    void setExportFile(String file) {
        exportFile = file;
    }
}

public class Main {

    PrintStream originalOut = System.out;
    DualPrintStreamLogger dualOut;
    UserInput ui;
    Commands c;

    public static void main(String[] args) {
        Main m = new Main();
        m.startLogger();
        m.getAction(args);
    }

    //turn on the dual Stream so System.out messages will go to both
    //the console and be corrected in an arraylist
    void startLogger() {
        dualOut = new DualPrintStreamLogger(System.out, new ArrayList<>());
        System.setOut(dualOut);
    }

    void executeCommand(String[] args) {
        String importFile = null;
        String exportFile = null;
        for (int i = 0; i < args.length; i+=2) {

                switch (args[i]) {
                    case "-import" -> {
                        if (i + 1 < args.length) {
                            importFile = args[i+1];
                        }else{
                            System.out.println("Import file needed.");
                        }
                    }
                    case "-export" -> {
                        if (i + 1 < args.length) {
                            exportFile = args[i+1];
                        }else{
                            System.out.println("Export file needed.");
                        }
                    }
                    default -> System.out.println("Only \"-import\" or \"-export\" commands are accepted.");
                }
        }

        if (importFile != null){
            c.executeImport(importFile);
        }
        if (exportFile != null){
            c.setExportFile(exportFile);
        }

    }

    void getAction(String[] args) {

        ui = new UserInput(dualOut);
        DeckCreator creator = new DeckCreator(ui);
        DeckStore store = new DeckStore(ui);
        Statistics stats = new Statistics(creator, dualOut);
        Logger logger = new Logger(dualOut);

        c = new Commands(creator, stats, store);

        if (args.length != 0) {
            executeCommand(args);
        }

        while (true) {
            String action = ui.getAction();
            switch (action) {
                case "add" -> creator.addCardToDeck();
                case "remove" -> creator.removeCardFromDeck(creator);
                case "import" -> creator.setDeck(store.getDeckFromFile(creator, ui.getImportFile()));
                case "export" -> store.storeDeckToFile(creator, stats, ui.getExportFile());
                case "ask" -> flashCards(creator, stats);
                case "log" -> logger.logConsole(ui.getLogFile(), false);
                case "hardest card" -> stats.printHardestCard(creator);
                case "reset stats" -> stats.resetStats(creator);
                case "exit" -> {
                    System.out.println("Bye bye!");
                    if (c.getExportFile() != null) {
                        c.executeExport();
                    }
                    //flush the reminder of the screen info to the file
                    logger.logConsole(UserInput.logFile, true);
                    ui.in.close();
                    //set System.out settings back tot he original settings
                    System.setOut(originalOut);
                    return;
                }
                default -> System.out.println("No such option.");
            }
        }
    }

    void flashCards(DeckCreator creator, Statistics stats) {
        String num = ui.getNumToAsk();
        int times = Integer.parseInt(num);

        if (creator.getDeck().isEmpty()){
            System.out.println("There are no cards in the deck.");
            return;
        }

        Map<String, DeckCreator> deck = creator.getDeck();

        Iterator<String> key = deck.keySet().iterator();
        for (int i = 0; i < times; i++) {
            if (!deck.isEmpty() && !key.hasNext()) {
                key = deck.keySet().iterator();
            }
            String nextKey = key.next();
            makeGuess(nextKey, creator, stats);
        }

    }

    void makeGuess(String key, DeckCreator creator, Statistics stats) {
        String guess = ui.getGuess(key);

        String definition = creator.getDef(key);
        if (isCorrectGuess(guess, definition)) {
            printCorrect();
        } else {
            incrementMistakeCounter(key, stats);
            printInCorrect(guess, definition, creator);
        }
    }

    void incrementMistakeCounter(String key, Statistics stats) {
        int mistakeCount = stats.getMistakes(key) + 1;
        stats.setMistakes(key, mistakeCount);
    }

    boolean isCorrectGuess(String guess, String definition) {
        if (definition == null) {
            return false;
        }
        return guess.equals(definition);
    }

    void printCorrect() {
        System.out.println("Correct!");
    }

    void printInCorrect(String guess, String definition, DeckCreator creator) {
        StringBuilder correction = new StringBuilder("\"Wrong. The right answer is \"")
                .append(definition)
                .append("\"");

        String front = "";
        Map<String, DeckCreator> deck = creator.getDeck();
        boolean isDefInDeck = false;

        for (String key : deck.keySet()) {
            if (creator.getDef(key) != null && creator.getDef(key).equals(guess)) {
                front = key;
                isDefInDeck = true;
                break;
            }
        }
        if (isDefInDeck) {
            correction.append(", but your definition is correct for \"")
                    .append(front)
                    .append("\".");
            System.out.println(correction);
        } else {
            correction.append(".");
            System.out.println(correction);
        }
    }
}

class UserInput {

    Scanner in = new Scanner(System.in);
    DualPrintStreamLogger dualOut;

    public UserInput(DualPrintStreamLogger dualOut) {
        this.dualOut = dualOut;
    }

    static String logFile = null;

    //store all log files created in a session
    static Set<String> logFiles = new HashSet<>();

    String[] actions = {
            "add",
            "remove",
            "import",
            "export",
            "ask",
            "exit",
            "log",
            "hardest card",
            "reset stats"
    };

    String getAction() {
        System.out.print("Input the action (");
        for (int i = 0; i < actions.length; i++) {
            System.out.print(actions[i]);
            if (i < actions.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("):");

        String action = in.nextLine();

        dualOut.logScannerInput(action);
        return action;
    }

    String getNumToAsk() {
        System.out.println("How many times to ask?");
        String num = in.nextLine();
        dualOut.logScannerInput(num);

        return num;
    }

    String getGuess(String key) {
        System.out.println("Print the definition of \"" + key + "\":");
        String guess = in.nextLine();
        dualOut.logScannerInput(guess);

        return guess;
    }

    String getCardToAdd() {
        System.out.println("The card:");
        String card = in.nextLine();
        dualOut.logScannerInput(card);

        return card;
    }

    String getDefinitionToAdd() {
        System.out.println("The definition of the card:");
        String definition = in.nextLine();
        dualOut.logScannerInput(definition);

        return definition;
    }

    String getCardToRemove() {
        System.out.println("Which card?");
        String card = in.nextLine();
        dualOut.logScannerInput(card);

        return card;
    }

    String getLogFile() {
        System.out.println("File name:");
        logFile = in.nextLine();
        dualOut.logScannerInput(logFile);
        logFiles.add(logFile);

        return logFile;
    }

    String getExportFile() {
        System.out.println("File name:");
        String exportFile = in.nextLine();
        dualOut.logScannerInput(exportFile);

        return exportFile;
    }

    String getImportFile() {
        System.out.println("File name:");
        String importFile = in.nextLine();
        dualOut.logScannerInput(importFile);

        return importFile;
    }
}

class DeckCreator {

    Map<String, DeckCreator> deck = new LinkedHashMap<>();
    String card;
    String def;
    int mistakes;

    UserInput ui;

    public DeckCreator(UserInput ui) {
        this.ui = ui;
        card = "";
        def = "";
        mistakes = 0;
    }

    //I chose a Map where the card is the key and the value
    //is the DeckCreator class, so that I can add as many statistcs as I
    //like to the card.
    public DeckCreator(String card, String def, int mistakes) {
        this.card = card;
        this.def = def;
        this.mistakes = mistakes;
    }

    void addCardToDeck() {

        String card = ui.getCardToAdd();

        if (deck.containsKey(card)) {
            System.out.println("The card \"" + card + "\" already exists.");
            return;
        }

        String definition = ui.getDefinitionToAdd();

        for (DeckCreator value : deck.values()) {
            if (value.def.equals(definition)) {
                System.out.println("The definition \"" + definition + "\" already exists.");
                return;
            }
        }

        deck.put(card, new DeckCreator(card, definition, 0));

        System.out.println("The pair (\"" + card + "\":" + "\"" + definition + "\") has been added.");

    }

    void removeCardFromDeck(DeckCreator creator) {
        Map<String, DeckCreator> deck = creator.getDeck();

        String card = ui.getCardToRemove();

        if (deck == null || deck.isEmpty() || !deck.containsKey(card)) {
            System.out.println("Can't remove \"" + card + "\": there is no such card.");
            return;
        }

        deck.remove(card);

        System.out.println("The card has been removed.");
    }

    void setDeck(Map<String, DeckCreator> deck) {
        if (deck != null) {
            this.deck = deck;
        }
    }

    Map<String, DeckCreator> getDeck() {
        return deck;
    }

    String getDef(String card) {
        if (deck.containsKey(card)) {
            return deck.get(card).def;
        }
        return null;
    }

}

class DeckStore {

    UserInput ui;
    String exportFile;
    String importFile;

    public DeckStore(UserInput ui) {
        this.ui = ui;
    }

    //store an in-memory hashmap deck to a file
    void storeDeckToFile(DeckCreator creator, Statistics stats, String exportFile) {

        Map<String, DeckCreator> deck = creator.getDeck();

        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Path.of(System.getProperty("user.dir"), exportFile), Charset.defaultCharset())) {
            for (String key : deck.keySet()) {
                bufferedWriter.write(key + "---" + creator.getDef(key) + "---" + stats.getMistakes(key));
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
        } catch (FileAlreadyExistsException alreadyExists) {
            System.out.println("A deck already exists with this name.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (deck != null) {
            System.out.println(deck.size() + " cards have been saved.");
        }
    }

    //import cards from file to in-memory hashmap
    Map<String, DeckCreator> getDeckFromFile(DeckCreator creator, String importFile) {

        AtomicReference<Map<String, DeckCreator>> deck = new AtomicReference<>();

        //points to current file directory automatically
        Path dirPath = Path.of((System.getProperty("user.dir")));

        //checks if importfile exists and if it does, passes on to
        //fileToDeck method to read cards into hashmap
        Path userFileName = Path.of(importFile);
        try (Stream<Path> walker = Files.walk(dirPath)) {
            walker.filter(f -> f.getFileName().equals(userFileName)).findFirst()
                    .ifPresentOrElse(p -> deck.set(fileToDeck(p, creator)), this::printError);
        } catch (IOException ignore) {
        }

        return deck.get();

    }

    void printError() {
        System.out.println("File not found.");
    }

    //does the work of reading in the cards and stats to in-memory hashmap
    Map<String, DeckCreator> fileToDeck(Path p, DeckCreator creator) {
        List<String> fileList;
        Map<String, DeckCreator> existingDeck = creator.getDeck();

        if (existingDeck == null) {
            existingDeck = new ConcurrentHashMap<>();
        }

        try {
            fileList = Files.readAllLines(p);
            for (int i = 0; i < fileList.size(); i++) {
                String[] terms = fileList.get(i).split("---");
                if (terms.length < 3 || !Validation.isValidNum(terms[2])) {
                    continue;
                }

                if (!existingDeck.isEmpty()) {
                    if (existingDeck.containsKey(terms[0])
                            && creator.getDef(terms[0]).equals(terms[1])
                    ) {
                        existingDeck.remove(terms[0]);
                    }
                }

                existingDeck.put(terms[0], new DeckCreator(terms[0], terms[1], Integer.parseInt(terms[2])));
            }
            System.out.println(fileList.size() + " cards have been loaded.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return existingDeck;
    }
}

class Validation {

    static boolean isValidNum(String num) {
        try {
            Integer.parseInt(num);
        } catch (NumberFormatException nfe) {
            System.out.println("Must be a number");
            return false;
        }
        return true;
    }
}

class Logger {

    DualPrintStreamLogger dualOut;
    String logFile = "";

    public Logger(DualPrintStreamLogger dualOut) {
        this.dualOut = dualOut;
    }

    //writes lines from arrayList which has been storing console lines,
    //into a .log or .txt file
    void logConsole(String currLogFile, boolean isFlush) {

        //if currLogFile is null than no log was requested
        if (currLogFile == null) {
            return;
        }

        //If a file already exists with that name, just write to it
        logFile = currLogFile;
        if (!isFlush) {
            System.out.println("The log has been saved.");
        }

        //If file doesn't exist, start writing it.
        //But if file already exists, continue to write to the same file by appending
        //the remaining lines to the existing file
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Path.of(System.getProperty("user.dir"), logFile), Charset.defaultCharset())) {
            for (String log : dualOut.getConsoleLogger()) {
                if (log.equals("\n")) {
                    bufferedWriter.newLine();
                }
                bufferedWriter.write(log);
            }
            bufferedWriter.flush();
        } catch (FileAlreadyExistsException alreadyExists) {
            for (String log : dualOut.getConsoleLogger()) {
                if (log.equals("\n")) {
                    continue;
                }
                try {
                    Files.writeString(Path.of(System.getProperty("user.dir"), logFile), log, Charset.defaultCharset());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}


class Statistics {
    DeckCreator creator;
    DualPrintStreamLogger dualOut;

    //class holds and statistics that will be added to the card.
    public Statistics(DeckCreator creator, DualPrintStreamLogger dualOut) {
        this.creator = creator;
        this.dualOut = dualOut;
    }

    void printHardestCard(DeckCreator creator) {
        Deque<String> highest = new ArrayDeque<>();
        highest.offerLast(creator.card);

        for (String key : creator.getDeck().keySet()) {
            if (!highest.isEmpty() && getMistakes(highest.peekLast()) > getMistakes(key)) {
                continue;
            }
            while (!highest.isEmpty() &&
                    getMistakes(highest.peekLast()) < getMistakes(key)) {
                highest.pollLast();
            }
            highest.offerLast(key);
        }

        int mostWrongCount = getMistakes(highest.peekLast());

        if (!highest.isEmpty() && mostWrongCount > 0) {
            int len = highest.size();
            if (len == 1) {  //if there is a single number that is the highest
                System.out.print("The hardest card is \"" + highest.pollFirst() + "\". ");
                System.out.println("You have " + mostWrongCount + " errors answering it.");
            } else {  //if multiple cards are tied for the highest number
                System.out.print("The hardest cards are ");
                while (!highest.isEmpty()) {
                    if (highest.size() == 1) {
                        System.out.print("\"" + highest.pollFirst() + "\".");
                    } else {
                        System.out.print("\"" + highest.pollFirst() + "\", ");
                    }
                }
                System.out.println(" You have " + mostWrongCount + " errors answering them.");
            }
        } else {
            System.out.println("There are no cards with errors.");
        }
    }

    void resetStats(DeckCreator creator) {
        for (String key : creator.getDeck().keySet()) {
            setMistakes(key, 0);
        }
        System.out.println("Card statistics have been reset.");
    }

    int getMistakes(String card) {
        if (creator.getDeck().containsKey(card)) {
            return creator.getDeck().get(card).mistakes;
        }
        return -1;

    }

    void setMistakes(String card, int count) {
        if (creator.getDeck().containsKey(card)) {
            creator.getDeck().get(card).mistakes = count;
        }
    }
}