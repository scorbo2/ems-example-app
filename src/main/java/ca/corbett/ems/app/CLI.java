package ca.corbett.ems.app;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Contains some static utility method for managing command line parameters.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public final class CLI {

    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final int DEFAULT_LISTENING_PORT = 1975;

    /**
     * Private constructor to avoid instantiation. *
     */
    private CLI() {
    }

    /**
     * Builds up and returns our expected command line options.
     *
     * @return An Options instance with our expected options.
     */
    public static Options buildOptions() {
        Options options = new Options();

        options.addOption(Option.builder("H")
                .longOpt("host")
                .hasArg()
                .argName("host")
                .desc("Host to connect to (default "+DEFAULT_HOSTNAME+")")
                .build());

        options.addOption(Option.builder("P")
                .longOpt("port")
                .hasArg()
                .argName("port")
                .desc("Port to use for connections (default " + DEFAULT_LISTENING_PORT + ")")
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show usage information and exit.")
                .build());

        options.addOption(Option.builder("v")
                .longOpt("version")
                .desc("Show version and exit.")
                .build());

        options.addOption(Option.builder()
                .longOpt("startServer")
                .desc("Start an EMS server")
                .build());

        options.addOption(Option.builder()
                .longOpt("startClient")
                .desc("Starts an EMS client")
                .build());

        options.addOption(Option.builder()
                .longOpt("startSubscriber")
                .desc("Starts a subscriber (requires --channel)")
                .build());

        options.addOption(Option.builder("C")
                .longOpt("channel")
                .hasArg()
                .argName("channel")
                .desc("Used with --startSubscriber, this is the channel to subscribe to.")
                .build());

        options.addOption(Option.builder("y")
                .longOpt("serverSpy")
                .desc("Optional with --startServer, outputs more log info.")
                .build());

        return options;
    }

    /**
     * Outputs usage information to stdout, and then optionally exits if instructed.
     *
     * @param options The expected command line options.
     * @param exit    if true, System.exit(0) is invoked after showing usage.
     */
    public static void showUsageText(Options options, boolean exit) {
        final HelpFormatter helpFormatter = new HelpFormatter();
        String note = "\nOne of --startServer, --startClient, or --startSubscriber is required.\nIf no args are given, a UI will be presented.";
        helpFormatter.printHelp(80, "java -jar EMS.jar [options]", "", options, note);

        if (exit) {
            System.exit(0);
        }
    }

    /**
     * Shorthand for showUsageText(buildOptions(), exit).
     *
     * @param exit If true, System.exit(0) is invoked after showing usage.
     */
    public static void showUsageText(boolean exit) {
        CLI.showUsageText(buildOptions(), exit);
    }

    /**
     * Generates a CommandLine using the given Options and arguments.
     *
     * @param options The Options instance to use (presumably from buildOptions)
     * @param args    The command line arguments to parse.
     * @return a CommandLine instance representing the given arguments.
     */
    public static CommandLine generateCommandLine(Options options, String[] args) {
        CommandLineParser parser = new DefaultParser();

        try {
            return parser.parse(options, args);
        } catch (ParseException pe) {
            System.out.println("ERROR: " + pe.getMessage());
            System.exit(1);
        }

        return null; // unreachable
    }

    /**
     * Shorthand for generateCommandLine(buildOptions(), args).
     *
     * @param args The command line arguments to parse.
     * @return A CommandLine instance representing the given arguments.
     */
    public static CommandLine generateCommandLine(String[] args) {
        return CLI.generateCommandLine(buildOptions(), args);
    }

}
