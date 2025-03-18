package ca.corbett.ems.app.handlers;

import ca.corbett.ems.handlers.AbstractCommandHandler;
import ca.corbett.ems.server.EMSServer;

import java.time.Duration;

/**
 * Reports how long the server has been up.
 *
 * @author scorbo2
 * @since 2023-11-24
 */
public class UptimeHandler extends AbstractCommandHandler {

    private final long startTime;

    public UptimeHandler() {
        super("UPTIME", "UP");
        startTime = System.currentTimeMillis();
    }

    @Override
    public int getMinParameterCount() {
        return 0;
    }

    @Override
    public int getMaxParameterCount() {
        return 0;
    }

    @Override
    public String getUsageText() {
        return name;
    }

    @Override
    public String getHelpText() {
        return "Reports how long the server has been up.";
    }

    @Override
    public String handle(EMSServer server, String clientId, String commandLine) {
        String msg = "Server has been up for ";
        Duration duration = Duration.ofMillis(System.currentTimeMillis() - startTime);
        long seconds = duration.getSeconds();
        long days = seconds / 86400;
        long HH = seconds / 3600;
        long MM = (seconds % 3600) / 60;
        long SS = seconds % 60;
        String daysUnit = days == 1 ? "day" : "days";
        String HHunit = HH == 1 ? "hour" : "hours";
        String MMunit = MM == 1 ? "minute" : "minutes";
        String SSunit = SS == 1 ? "second" : "seconds";

        if (days == 0 && HH == 0 && MM == 0) {
            msg += String.format("%d %s", SS, SSunit);
        } else if (days == 0 && HH == 0) {
            msg += String.format("%d %s and %d %s", MM, MMunit, SS, SSunit);
        } else if (days == 0) {
            msg += String.format("%d %s, %d %s, and %d %s", HH, HHunit, MM, MMunit, SS, SSunit);
        } else {
            msg += String.format("%d %s, %d %s, and %d %s", days, daysUnit, HH, HHunit, MM, MMunit);
        }

        return createOkResponse(msg);
    }

}
