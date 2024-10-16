package potato.modsupport;

import potato.Logger;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModLoader {
    private static final String MODS_FOLDER = "mods";
    private final List<Mod> loadedMods = new ArrayList<>();
    private final Logger logger = new Logger(this.getClass().getName());

    public void loadMods() {
        File modsFolder = new File(MODS_FOLDER);
        if (!modsFolder.exists() || !modsFolder.isDirectory()) {
            System.out.println("Mods folder not found or is not a directory.");
            return;
        }

        File[] jarFiles = modsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null) {
            System.out.println("No jar files found in the mods folder.");
            return;
        }

        for (File jarFile : jarFiles) {
            loadModFromJar(jarFile);
        }
    }

    private void loadModFromJar(File jarFile) {
        try {
            URL[] urls = {jarFile.toURI().toURL()};

            try (URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
                 JarFile jar = new JarFile(jarFile)) {

                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                        Class<?> clazz = classLoader.loadClass(className);

                        if (Mod.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            Mod mod = (Mod) clazz.getDeclaredConstructor().newInstance();
                            loadedMods.add(mod);
                            mod.init();
                            System.out.println("Loaded mod: " + className + " from " + jarFile.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading mod from " + jarFile.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateMods() {
        for (Mod mod : loadedMods) {
            mod.update();
        }
    }

    public List<Mod> getLoadedMods() {
        return loadedMods;
    }
}