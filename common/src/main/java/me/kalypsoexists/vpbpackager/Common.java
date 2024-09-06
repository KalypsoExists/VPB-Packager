package me.kalypsoexists.vpbpackager;

import com.google.gson.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Common {

    public static final String MOD_ID = "vpbpackager";
    public static final String MOD_NAME = "VPBPackager";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
    private static Path packsDirectory;
    private static Path modsDirectory;

    // pack name, version, file
    private static final List<Pack> packs = new ArrayList<>();

    // pack name, version
    private static final List<Pack> loadedPacks = new ArrayList<>();

    public static void init(Path gameDir) {
        modsDirectory = gameDir.resolve("mods");
        final File packDir = gameDir.resolve("pointblank").toFile();
        if(loadModFiles()) {
            if(!packDir.exists()) {
                boolean mkDir = packDir.mkdir();
                if(mkDir) {
                    packsDirectory = packDir.toPath();
                    LOG.info("Created .../pointblank/");
                } else LOG.warn("Failed to create pack directory .../pointblank/");
            }
            packsDirectory = packDir.toPath();
            loadExistingPackFiles();
            loadPacks();
        }

    }



    private static void loadPacks() {
        for(Pack pack : packs) {

            String[] content = pack.getContent();
            File file = pack.getFile();

            String rawName = content[0]+"-"+content[1];
            String name = content[0].replaceAll("[^0-9A-Za-z_-]", "_");
            String nameVersion = rawName.replaceAll("[^0-9A-Za-z_-]", "_");

            Pack loadedPack = getWithName(content[0]);
            if(loadedPack != null) {
                int currentVersion = Integer.parseInt(loadedPack.getContent()[1].replaceAll("[^0-9]", ""));
                int loadingVersion = Integer.parseInt(content[1].replaceAll("[^0-9]", ""));
                if(currentVersion == loadingVersion) {
                    LOG.info("Skipping loading "+nameVersion+" as already loaded.");
                    continue; // The pack version is equal to that of loaded.
                } else if(currentVersion > loadingVersion) {
                    LOG.info("Skipping loading "+nameVersion+" as already loaded higher version "+loadedPack.getContent()[1]);
                    continue; // The pack version is lower than that of loaded.
                }
            }

            LOG.info("Copying ["+rawName+"]");

            try {
                File checkZip = new File(packsDirectory+File.separator+name+".zip");
                File checkDirectory = new File(packsDirectory+File.separator+name);

                if(checkZip.isFile() && checkZip.exists()) {
                    FileUtils.delete(checkZip);
                }
                if(checkDirectory.isDirectory() && checkDirectory.exists()) {
                    FileUtils.deleteDirectory(checkDirectory);
                }
            } catch(IOException e) {
                LOG.info("Failed to delete existing old version of the pack ["+rawName+"]");
                LOG.info("Skipping ["+rawName+"]");
            }


            try {

                FileUtils.copyFileToDirectory(file, packsDirectory.toFile());
                File copied = new File(packsDirectory +File.separator+file.getName());
                File zipFile = new File(copied.getAbsolutePath().replace(file.getName(), name+".zip"));
                if (copied.renameTo(zipFile)) {
                    LOG.info("File extension changed successfully");
                } else {
                    LOG.error("Failed to change the file extension.");
                }
            } catch (IOException e) {
                LOG.error("Failed to load pack ["+rawName+"]");
            }

        }
    }

    private static Pack getWithName(String packName) {
        for(Pack pack : loadedPacks) {
            if(pack.getContent()[0].equals(packName)) {
                return pack;
            }
        }
        return null;
    }

    private static boolean loadModFiles() {

        LOG.info("Loading pack files from .../mods/");

        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(modsDirectory);

            if(stream == null) return false;

            for (Path entry : stream) {
                if (!Files.isDirectory(entry) && entry.toString().endsWith(".jar")) {
                    File file = entry.toFile();
                    Pack pack = checkPackZip(file);
                    if(pack == null) continue;
                    LOG.info("Detected valid pack "+file.getName());
                    packs.add(pack);
                }
            }

            if(packs.isEmpty()) {
                LOG.warn("No packs were loaded from .../mods/");
                return false;
            }

            return true; // successful checking
        } catch(IOException e) {
            LOG.error("An error occurred trying to load mods(packs) directory");
        }

        return false;
    }

    private static boolean loadExistingPackFiles() {

        LOG.info("Loading existing pack files");

        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(packsDirectory);

            if(stream == null) return false;

            for (Path entry : stream) {
                File file = entry.toFile();
                Pack pack = null;

                if (!Files.isDirectory(entry) && entry.toString().endsWith(".zip"))
                    pack = checkPackZip(file);
                else if (Files.isDirectory(entry))
                    pack = checkPackFolder(file);

                // Skip mods / the ones without ext.json
                if (pack == null) return false;
                loadedPacks.add(pack);
            }

            if(loadedPacks.isEmpty()) {
                LOG.warn("No packs already exist in .../pointblank/");
                return false;
            }

            return true; // successful checking
        } catch(IOException e) {
            LOG.error("An error occurred trying to load pointblank(packs) directory");
        }

        return false;
    }

    private static Pack checkPackZip(File file) {

        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> e = zipFile.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();

                if (!entry.isDirectory() && entry.getName().equals("ext.json")) {
                    BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                    String json = IOUtils.toString(bis, "UTF-8");
                    bis.close();

                    String[] content = readPackJson(json);
                    if(content == null) return null;

                    return new Pack(content, file);
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to read ext.json in "+file);
        }


        return null;
    }

    private static Pack checkPackFolder(File folder) {
        try {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().equals("ext.json")) {
                        byte[] fileBytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                        String[] content = readPackJson(new String(fileBytes, StandardCharsets.UTF_8));

                        if(content == null) return null;
                        return new Pack(content, file);
                    }
                }
            }
        } catch(IOException e) {
            LOG.error("Failed to read ext.json in "+folder);
        }

        return null;
    }



    private static String[] readPackJson(String json) {

        JsonElement jsonElement = JsonParser.parseString(json);
        JsonObject object = jsonElement.getAsJsonObject();
        String packName = object.get("name").getAsString();
        String packVersion = object.get("version").getAsString();

        if(packName == null || packVersion == null) return null;

        return new String[]{packName, packVersion};
    }

}