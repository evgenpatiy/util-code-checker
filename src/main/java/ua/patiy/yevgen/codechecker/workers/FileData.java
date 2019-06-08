package ua.patiy.yevgen.codechecker.workers;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileData {
    private boolean tab;
    private String fileName;
    private long lines;
}
