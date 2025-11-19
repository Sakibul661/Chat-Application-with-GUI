public class ExceptionHandler {

    public static void handle(String context, Exception e) {
        String message = "[Exception] " + context + " -> " + (e == null ? "null" : e.getMessage());
        System.err.println(message);
        ServerLogger.log(message);
    }

    public static void handleWarning(String warningMessage) {
        System.out.println("[Warning] " + warningMessage);
        ServerLogger.log("[Warning] " + warningMessage);
    }
}
