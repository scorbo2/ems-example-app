package ca.corbett.ems.app;

import ca.corbett.extras.about.AboutInfo;

public final class Version {

    public static final String VERSION = "1.0.0";
    public static final String NAME = "EMS Example App";
    public static final String FULL_NAME = NAME + " " + VERSION;
    public static final String PROJECT_URL = "https://github.com/scorbo2/ems-example-app";
    public static final String COPYRIGHT = "Copyright © 2023 Steve Corbett";
    public static final String LICENSE = "https://opensource.org/license/mit";

    public static final AboutInfo aboutInfo;

    static {
        aboutInfo = new AboutInfo();
        aboutInfo.applicationName = NAME;
        aboutInfo.applicationVersion = VERSION;
        aboutInfo.projectUrl = PROJECT_URL;
        aboutInfo.license = LICENSE;
        aboutInfo.copyright = COPYRIGHT;
    }
}
