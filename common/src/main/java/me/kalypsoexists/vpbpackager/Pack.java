package me.kalypsoexists.vpbpackager;

import java.io.File;

public class Pack {

    private final String[] content;
    private final File file;

    public Pack(String[] content, File file) {
        this.content = content;
        this.file = file;
    }

    public String[] getContent() {
        return content;
    }

    public File getFile() {
        return file;
    }

}

