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

    // Constants
    public static final String MOD_ID = "vpbpackager";
    public static final String MOD_NAME = "VPB-Packager";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    // ./pointblank
    private static Path packsDirectory;
    // ./mods
    private static Path modsDirectory;

    // Mods detected in ./mods
    private static final List<Pack> packs = new ArrayList<>();
    // Mods detected in ./pointblank
    private static final List<Pack> loadedPacks = new ArrayList<>();
    // Packs to load after all mods are loaded by fml
    private static final List<Pack> packsToLoad = new ArrayList<>();

    public static void init(Path gameDir) {

        // Directories
        modsDirectory = gameDir.resolve("mods");
        final File packDir = gameDir.resolve("pointblank").toFile();

        // Only continue if any packs are detected
        if(loadModFiles()) {
            // ./pointblank
            if(!packDir.exists()) {
                if(packDir.mkdir()) LOG.info("Created ./pointblank");
                else LOG.warn("Failed to create pack directory ./pointblank");
            }
            packsDirectory = packDir.toPath();

            loadExistingPackFiles();
            loadPacks();
        }
    }

    public static void loadComplete() {
        List<Pack> del = new ArrayList<>();
        List<Pack> copy = new ArrayList<>();
        for(Pack pack : packsToLoad) {
            if(pack.getLoadedPack() != null) del.add(pack);
            copy.add(pack);
        }
        deletePacks(del);
        copyPacks(copy);
    }

    private static void deletePacks(List<Pack> packs) {
        for(Pack pack : packs) {
            try {
                File del = pack.getLoadedPack();
                if(del.isDirectory()) FileUtils.deleteDirectory(del);
                if(del.isFile()) FileUtils.delete(del);
            } catch(IOException e) {
                LOG.info("Failed to delete existing old/existing version of the pack ["+pack.getContent()[0]+"]");
            }
        }
    }

    private static void copyPacks(List<Pack> packs) {
        for(Pack pack : packs) {
            String name = pack.getContent()[0];
            try {
                File file = pack.getFile();
                FileUtils.copyFileToDirectory(file, packsDirectory.toFile());
                File copied = new File(packsDirectory +File.separator+file.getName());
                File zipFile = new File(copied.getAbsolutePath().replace(file.getName(), file.getName().replaceFirst("\\.jar$", "")+".zip"));
                if (copied.renameTo(zipFile)) {
                    LOG.info("Successfully moved ["+name+"]");
                } else {
                    LOG.error("Failed to move ["+name+"]");
                }
            } catch (IOException e) {
                LOG.error("Failed to load pack ["+name+"]");
            }
        }
    }

    // Load packs from ./mods
    private static void loadPacks() {
        for(Pack pack : packs) {

            String[] content = pack.getContent();
            Pack loadedPack = getLoadedPack(content[0]);

            String nameVersion = content[0]+"-"+content[1];
            //String name = content[0].replaceAll("[^0-9a-z/._-]", "_");

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

                pack.loadedPack(loadedPack.getFile());
                packsToLoad.add(pack);
                LOG.info("Added pack to be updated ["+nameVersion+"]");
                continue;
            }

            /*try {
                File del = loadedPack.getFile();
                if(del.isDirectory()) FileUtils.deleteDirectory(del);
                if(del.isFile()) FileUtils.delete(del);
            } catch(IOException e) {
                LOG.info("Failed to delete existing old/existing version of the pack ["+rawName+"]");
            }*/

            LOG.info("Added pack to be loaded ["+nameVersion+"]");
            packsToLoad.add(pack);

            /*try {

                FileUtils.copyFileToDirectory(file, packsDirectory.toFile());
                File copied = new File(packsDirectory +File.separator+file.getName());
                File zipFile = new File(copied.getAbsolutePath().replace(file.getName(), file.getName().replaceFirst("\\.jar$", "")+".zip"));
                if (copied.renameTo(zipFile)) {
                    LOG.info("File extension changed successfully");
                } else {
                    LOG.error("Failed to change the file extension.");
                }
            } catch (IOException e) {
                LOG.error("Failed to load pack ["+rawName+"]");
            }*/

        }
    }

    // To check if a pack already exists in ./pointblank
    private static Pack getLoadedPack(String packName) {
        for(Pack pack : loadedPacks) {
            if(pack.getContent()[0].equals(packName)) {
                return pack;
            }
        }
        return null;
    }

    // Detect packs from ./mods
    private static boolean loadModFiles() {

        LOG.info("Loading pack files from ./mods");

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
                LOG.warn("No packs were loaded from ./mods");
                return false;
            }

            return true;
        } catch(IOException e) {
            LOG.error("An error occurred trying to load mods(packs) directory");
        }

        return false;
    }

    // Detect packs in ./pointblank
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
                LOG.warn("No packs already exist in ./pointblank");
                return false;
            }

            return true; // successful checking
        } catch(IOException e) {
            LOG.error("An error occurred trying to load packs directory");
        }

        return false;
    }

    // Check ext.json in zip content pack
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

    // Check ext.json in folder content pack
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

    // Read json for ext.json with gson
    private static String[] readPackJson(String json) {

        JsonElement jsonElement = JsonParser.parseString(json);
        JsonObject object = jsonElement.getAsJsonObject();
        String packName = object.get("name").getAsString();
        String packVersion = object.get("version").getAsString();

        if(packName == null || packVersion == null) return null;

        return new String[]{packName, packVersion};
    }

}