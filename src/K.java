// src/K.java
public class K {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java K <input file>");
            System.exit(1);
        }

        String inputFile = args[0];

        try {
            // Scanner constructor expects a String filename
            Scanner scanner = new Scanner(inputFile);

            // Parser constructor expects a Scanner
            Parser parser = new Parser(scanner);

            // Parse the input
            parser.Parse();

            System.out.println("Parsing completed.");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
