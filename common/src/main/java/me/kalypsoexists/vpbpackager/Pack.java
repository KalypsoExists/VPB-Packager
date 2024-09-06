package me.kalypsoexists.vpbpackager;

import java.io.File;

public class Pack {

    private final String[] content;
    private final File file;
    private File loadedPackFile = null;

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

    public void loadedPack(File pack) {
        loadedPackFile = pack;
    }

    public File getLoadedPack() {
        return loadedPackFile;
    }

}

