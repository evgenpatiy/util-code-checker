package ua.patiy.yevgen.codechecker.workers;

import lombok.Getter;

public class OSValidator {
    public enum OS {
        MAC, WINDOWS, UNIX, SOLARIS
    }

    @Getter
    private OS env;

    public OSValidator() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            env = OS.WINDOWS;
        } else if (osName.contains("mac")) {
            env = OS.MAC;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("bsd") || osName.contains("irix")
                || osName.contains("aix") || osName.contains("hp-ux")) {
            env = OS.UNIX;
        } else if (osName.contains("sunos") || osName.contains("solaris")) {
            env = OS.SOLARIS;
        } else {
            env = null;
            System.out.printf("%s%n", "Your operating system not supported, exiting...");
            System.exit(1);
        }
    }
}
