package com.uwdie.stafftools.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * File-based profile exchange.
 *
 * Export opens a native "save file" window (falls back to the mod's
 * profile folder), import reads {@code .json} files from
 * {@code <config>/stafftools-profiles} and can open that folder in the
 * OS file manager.
 */
public final class ProfileIO {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String DIR_NAME = "stafftools-profiles";

    private static Path cachedDir;

    private ProfileIO() {
    }

    /** {@code <configDir>/stafftools-profiles}, created on demand. */
    public static Path profilesDir() {

        if (cachedDir != null) {
            return cachedDir;
        }

        cachedDir = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(DIR_NAME);

        try {
            Files.createDirectories(cachedDir);
        } catch (IOException e) {

            System.err.println(
                    "[StaffTools] Cannot create " + DIR_NAME + " folder."
            );
        }

        return cachedDir;
    }

    /** Every {@code .json} file inside the profile folder. */
    public static List<String> listFiles() {

        List<String> names = new ArrayList<>();

        try (var stream = Files.list(profilesDir())) {

            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .sorted()
                    .forEach(names::add);

        } catch (IOException e) {

            System.err.println(
                    "[StaffTools] Cannot read " + DIR_NAME + " folder."
            );
        }

        return names;
    }

    /**
     * Writes the profile to disk. Tries the native save dialog first so
     * the user picks the location; on a headless / failed AWT it falls
     * back to the mod profile folder.
     *
     * @return the written file, or empty when the dialog was cancelled
     */
    public static Optional<Path> export(Profile profile) {

        String json = GSON.toJson(exportCopy(profile));

        String defaultName = safeName(profile.getName());

        Path target = askSavePath(defaultName)
                .orElseGet(() ->
                        profilesDir().resolve(defaultName)
                );

        try {

            Files.createDirectories(target.getParent());

            Files.writeString(target, json, StandardCharsets.UTF_8);

            return Optional.of(target);

        } catch (IOException e) {

            System.err.println(
                    "[StaffTools] Failed to write the profile file."
            );

            e.printStackTrace();

            return Optional.empty();
        }
    }

    /** Native save dialog; empty when AWT is missing or cancelled. */
    private static Optional<Path> askSavePath(String defaultName) {

        if (GraphicsEnvironment.isHeadless()) {
            return Optional.empty();
        }

        Frame frame = null;

        try {

            frame = new Frame();

            FileDialog dialog = new FileDialog(
                    frame,
                    "Save profile",
                    FileDialog.SAVE
            );

            dialog.setFile(defaultName);
            dialog.setVisible(true);

            String directory = dialog.getDirectory();
            String file = dialog.getFile();

            if (directory == null || file == null) {
                return Optional.empty();
            }

            return Optional.of(
                    Path.of(directory, file)
            );

        } catch (Throwable t) {

            return Optional.empty();

        } finally {

            if (frame != null) {
                frame.dispose();
            }
        }
    }

    /**
     * Opens the profile folder in the OS file manager.
     *
     * Tries, in order: Desktop, {@code explorer /select}, {@code start},
     * {@code xdg-open}, {@code open}. Minecraft's dev launcher often runs
     * with AWT disabled, so the Desktop route alone is not enough.
     */
    public static boolean openProfilesFolder() {

        Path dir = profilesDir();

        if (Desktop.isDesktopSupported()) {

            try {

                Desktop.getDesktop().open(dir.toFile());

                return true;

            } catch (Throwable ignored) {

                // fall through to the platform commands
            }
        }

        String path = dir.toAbsolutePath().toString();

        String[][] commands = {

                { "cmd", "/c", "explorer /select," + path },
                { "cmd", "/c", "start", "", path },
                { "xdg-open", path },
                { "open", path },
        };

        for (String[] command : commands) {

            try {

                new ProcessBuilder(command)
                        .redirectErrorStream(true)
                        .start();

                return true;

            } catch (IOException ignored) {

                // try the next one
            }
        }

        return false;
    }

    /** Parses a profile file from the mod profile folder. */
    public static Profile importFromFile(String fileName) {

        Path file = profilesDir().resolve(fileName);

        if (!Files.isRegularFile(file)) {
            return null;
        }

        try {

            String json = Files.readString(
                    file,
                    StandardCharsets.UTF_8
            );

            return parse(json);

        } catch (IOException | JsonSyntaxException e) {

            return null;
        }
    }

    /** Parses profile JSON text (kept for clipboard-style sharing). */
    public static Profile parse(String json) {

        if (json == null || json.isBlank()) {
            return null;
        }

        try {

            Profile loaded = GSON.fromJson(json, Profile.class);

            if (loaded == null) {
                return null;
            }

            if (loaded.getName() == null ||
                    loaded.getName().isBlank()) {

                loaded.setName("Imported");
            }

            loaded.assignNewId();

            if (loaded.getMacros().isEmpty() &&
                    loaded.getActionEntries().isEmpty()) {

                return null;
            }

            return loaded;

        } catch (JsonSyntaxException e) {

            return null;
        }
    }

    public static String exportText(Profile profile) {
        return GSON.toJson(exportCopy(profile));
    }

    /** Copy with a fresh id, so an imported profile never collides. */
    private static Profile exportCopy(Profile profile) {

        Profile clone = profile.copy();

        clone.assignNewId();
        clone.setName(profile.getName());

        return clone;
    }

    private static String safeName(String name) {

        String base = name == null || name.isBlank()
                ? "profile"
                : name.trim();

        StringBuilder builder = new StringBuilder();

        for (char c : base.toCharArray()) {

            boolean ok = Character.isLetterOrDigit(c)
                    || c == '-'
                    || c == '_'
                    || c == ' ';

            builder.append(ok ? c : '_');
        }

        String result = builder.toString().trim();

        return (result.isEmpty() ? "profile" : result) + ".json";
    }

    /** "stafftools-profiles" folder name, for toasts / hints. */
    public static String folderName() {
        return profilesDir().toString();
    }
}
