package com.pclewis.mcpatcher;

import java.io.File;
import java.util.ArrayList;

abstract class UserInterface {
    abstract boolean shouldExit();

    void show() {
    }

    File chooseMinecraftDir(File enteredMCDir) {
        return null;
    }

    boolean locateMinecraftDir(String enteredMCDir) {
        ArrayList<File> mcDirs = new ArrayList<File>();
        if (enteredMCDir == null) {
            mcDirs.add(MCPatcherUtils.getDefaultGameDir());
            mcDirs.add(new File("."));
            mcDirs.add(new File(".."));
        } else {
            mcDirs.add(new File(enteredMCDir).getAbsoluteFile());
        }

        for (File dir : mcDirs) {
            if (MCPatcherUtils.setGameDir(dir)) {
                return true;
            }
        }

        File minecraftDir = mcDirs.get(0);
        while (true) {
            minecraftDir = chooseMinecraftDir(minecraftDir);
            if (minecraftDir == null) {
                return false;
            }
            if (MCPatcherUtils.setGameDir(minecraftDir) ||
                MCPatcherUtils.setGameDir(minecraftDir.getParentFile())) {
                return true;
            }
        }
    }

    boolean go() {
        File defaultMinecraft = MCPatcherUtils.getMinecraftPath("bin", "minecraft.jar");
        if (!defaultMinecraft.exists()) {
            setBusy(false);
            return false;
        } else if (MCPatcher.setMinecraft(defaultMinecraft, true)) {
            updateModList();
            return true;
        } else {
            Logger.log(Logger.LOG_MAIN, "ERROR: %s missing or corrupt", defaultMinecraft.getPath());
            showCorruptJarError();
            setBusy(false);
            return false;
        }
    }

    void updateProgress(int value, int max) {
    }

    void setModList(ModList modList) {
    }

    void updateModList() {
    }

    void setBusy(boolean busy) {
    }

    void showBetaWarning() {
    }

    void showCorruptJarError() {
    }

    static class GUI extends UserInterface {
        private MainForm mainForm = new MainForm();

        @Override
        boolean shouldExit() {
            return false;
        }

        @Override
        void show() {
            mainForm.show();
        }

        @Override
        File chooseMinecraftDir(File enteredMCDir) {
            return mainForm.chooseMinecraftDir(enteredMCDir);
        }

        @Override
        void updateProgress(int value, int max) {
            mainForm.updateProgress(value, max);
        }

        @Override
        void setModList(ModList modList) {
            mainForm.setModList(modList);
        }

        @Override
        void updateModList() {
            mainForm.updateModList();
        }

        @Override
        void setBusy(boolean busy) {
            mainForm.setBusy(busy);
        }

        @Override
        void showBetaWarning() {
            mainForm.showBetaWarning();
        }

        @Override
        void showCorruptJarError() {
            mainForm.showCorruptJarError();
        }
    }

    static class CLI extends UserInterface {
        @Override
        boolean shouldExit() {
            return true;
        }

        @Override
        boolean go() {
            if (!super.go()) {
                return false;
            }
            boolean ok = false;
            try {
                MCPatcher.getApplicableMods();
                System.out.println();
                System.out.println("#### Class map:");
                MCPatcher.showClassMaps(System.out);
                if (MCPatcher.patch()) {
                    ok = true;
                }
                System.out.println();
                System.out.println("#### Patch summary:");
                MCPatcher.showPatchResults(System.out);
            } catch (Throwable e) {
                Logger.log(e);
            }
            return ok;
        }
    }
}
