package ca.corbett.ems.app.handlers;

import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

import java.util.List;

import static ca.corbett.ems.server.EMSServer.DELIMITER;
import static ca.corbett.ems.server.EMSServer.UNRECOGNIZED_COMMAND;

/**
 * Lists all commands registered on this EMS server along with their help usage text.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class HelpHandler extends AbstractCommandHandler {

    public HelpHandler() {
        super("help", "?");
    }

    @Override
    public int getMinParameterCount() {
        return 0;
    }

    @Override
    public int getMaxParameterCount() {
        return 1;
    }

    @Override
    public String getHelpText() {
        return "Lists available commands, or shows detailed help for a specific command.";
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {

        // show help for a specific command
        if (commandLine.contains(DELIMITER)) {
            String command = getAllFieldsToEndOfLine(commandLine, 1, DELIMITER);
            AbstractCommandHandler handler = server.getCommandHandler(command);
            if (handler == null) {
                return UNRECOGNIZED_COMMAND;
            }
            return handler.getHelpText()
                    + "\nUSAGE: "
                    + handler.getUsageText()
                    + "\n"
                    + createOkResponse();
        }

        // List all commands:
        else {
            StringBuilder sb = new StringBuilder();
            List<String> commands = server.listCommands();
            for (String cmd : commands) {
                sb.append(cmd);
                sb.append("\n");
            }
            sb.append(createOkResponse());
            return sb.toString();
        }
    }

    @Override
    public String getUsageText() {
        return name + "[" + DELIMITER + "<command>]";
    }
}
