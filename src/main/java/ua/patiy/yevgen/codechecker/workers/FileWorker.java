package ua.patiy.yevgen.codechecker.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.apache.tika.Tika;

public class FileWorker {
    private final String tabReplacer = "    ";
    private final char tab = '\u0009';

    public String getFileType(File file) throws IOException {
        return new Tika().detect(file);
    }

    public String getFileTime(File file) throws IOException {
        FileTime time = Files.getLastModifiedTime(Paths.get(file.getAbsolutePath()), LinkOption.NOFOLLOW_LINKS);
        Instant acsessTime = time.toInstant();
        ZonedDateTime zdt = acsessTime.atZone(ZoneId.of("UTC"));
        return DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm:ss").format(zdt);
    }

    public String getPermissions(Set<PosixFilePermission> perm) {
        String s = "-";

        if (perm.contains(PosixFilePermission.OWNER_READ)) {
            s += "r";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.OWNER_WRITE)) {
            s += "w";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.OWNER_EXECUTE)) {
            s += "x";
        } else {
            s += "-";
        }
        s += "/";
        if (perm.contains(PosixFilePermission.GROUP_READ)) {
            s += "r";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.GROUP_WRITE)) {
            s += "w";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.GROUP_EXECUTE)) {
            s += "x";
        } else {
            s += "-";
        }
        s += "/";

        if (perm.contains(PosixFilePermission.OTHERS_READ)) {
            s += "r";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.OTHERS_WRITE)) {
            s += "w";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.OTHERS_EXECUTE)) {
            s += "x";
        } else {
            s += "-";
        }
        return s;
    }

    public long countLines(Path path) throws IOException {
        return Files.lines(path).count();
    }

    public String fileToString(Path path) throws FileNotFoundException, IOException { // fast file reader
        String code = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toString()))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                code += line + System.lineSeparator();
            }
        }
        return code;
    }

    public void stringToFile(String code, Path path) throws IOException { // fast file writer
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toString(), false))) {
            writer.write(code);
        }
    }

    public boolean hasTabs(Path path) throws FileNotFoundException, IOException {
        return fileToString(path).contains(String.valueOf(tab));
    }

    public String replaceInCodeString(String code, String original, String target) {
        return code.replaceAll(original, target);
    }

    public void fixTabs(Path path) throws IOException {
        String fixedCode = replaceInCodeString(fileToString(path), String.valueOf(tab), tabReplacer);
        Files.move(path, Paths.get(path.toString() + ".old"), StandardCopyOption.REPLACE_EXISTING); // backup original file
        stringToFile(fixedCode, path);
    }
}
