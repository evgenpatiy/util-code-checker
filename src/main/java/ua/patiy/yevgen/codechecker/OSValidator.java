package ua.patiy.yevgen.codechecker;

import lombok.Getter;

public class OSValidator {
    public enum OS {
        MAC, WINDOWS, UNIX, SOLARIS
    }

    @Getter
    private OS env;

    public OSValidator() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.indexOf("win") >= 0) {
            env = OS.WINDOWS;
        } else if (osName.indexOf("mac") >= 0) {
            env = OS.MAC;
        } else if (osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") > 0) {
            env = OS.UNIX;
        } else if (osName.indexOf("sunos") >= 0) {
            env = OS.SOLARIS;
        } else {
            env = null;
            System.out.printf("%s%n", "Your operating system not supported, exiting...");
            System.exit(1);
        }
    }
}
