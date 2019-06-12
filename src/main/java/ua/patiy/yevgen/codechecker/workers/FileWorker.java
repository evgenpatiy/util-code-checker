package ua.patiy.yevgen.codechecker.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.tika.Tika;

import lombok.Getter;
import lombok.Setter;

public class FileWorker {
    @Getter
    @Setter
    public class Space {
        private long totalSpace;
        private long availableSpace;
    }

    private final String tabReplacer = "    ";
    private final char tab = '\u0009';

    public Map<Path, Space> getSpace() {
        Map<Path, Space> result = new LinkedHashMap<Path, Space>();
        NumberFormat nf = NumberFormat.getNumberInstance();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            System.out.print(root + ": ");
            try {
                Space space = new Space();
                FileStore store = Files.getFileStore(root);
                System.out.println("available=" + nf.format(store.getUsableSpace()) + ", total="
                        + nf.format(store.getTotalSpace()));
                space.setAvailableSpace(store.getUsableSpace());
                space.setTotalSpace(store.getTotalSpace());
                result.put(root, space);
            } catch (IOException e) {
                System.out.println("error querying space: " + e.toString());
            }
        }
        return result;
    }

    public String getFileType(File file) throws IOException {
        return new Tika().detect(file);
    }

    public String getFileTime(File file) throws IOException {
        FileTime time = Files.getLastModifiedTime(Paths.get(file.getAbsolutePath()), LinkOption.NOFOLLOW_LINKS);
        Instant acsessTime = time.toInstant();
        ZonedDateTime t = acsessTime.atZone(ZoneId.of("UTC"));
        return DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm:ss").format(t);
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

    public long countLines(Path file) throws IOException {
        return Files.lines(file).count();
    }

    public boolean hasTabs(Path file) throws FileNotFoundException, IOException {
        try (InputStream in = new FileInputStream(file.toFile());
                Reader reader = new InputStreamReader(in);
                BufferedReader buffer = new BufferedReader(reader)) {
            int r;
            while ((r = buffer.read()) != -1) {
                if ((char) r == '\t') {
                    buffer.close();
                    return true;
                }
            }
        }
        return false;
    }

    public void fixTabs(Path file) throws IOException {
        Path oldFile = Files.move(file, Paths.get(file.toString() + ".old"), StandardCopyOption.REPLACE_EXISTING);
        try (BufferedReader readBuffer = new BufferedReader(
                new InputStreamReader(new FileInputStream(oldFile.toFile())));
                BufferedWriter writeBuffer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(file.toFile())))) {
            int r;
            while ((r = readBuffer.read()) != -1) {
                if ((char) r == tab) {
                    writeBuffer.write(tabReplacer);
                } else {
                    writeBuffer.write(r);
                }
            }
        }
    }
}
