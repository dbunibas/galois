package queryexecutor.utility;

public class PrintUtility {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    public static void printError(String message) {
        System.out.println(ANSI_RED + message + ANSI_RESET);
    }

    public static void printWarning(String message) {
        System.out.println(ANSI_YELLOW + message + ANSI_RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(ANSI_GREEN + message + ANSI_RESET);
    }

    public static void printInformation(String message) {
        System.out.println(ANSI_BLUE + message + ANSI_RESET);
    }

    public static void printMessage(String message) {
//        System.out.println(ANSI_BLACK + message + ANSI_RESET);
        System.out.println(message);
    }

    public static void printPercentage(int percentage) {
        System.out.print("[");
        for (int i = 0; i < 10; i++) {
            if (percentage < 33) {
                System.out.print(ANSI_RED);
            } else if (percentage < 66) {
                System.out.print(ANSI_YELLOW);
            } else {
                System.out.print(ANSI_GREEN);
            }
            if (i * 10 < percentage) {
                System.out.print("#");
            } else {
                System.out.print(" ");
            }
        }
        System.out.print(ANSI_RESET);
        System.out.print("] " + percentage + "%\r");
    }
}
