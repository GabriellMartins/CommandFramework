package org.github.gabrielmartins.command;

import org.bukkit.plugin.java.JavaPlugin;
import org.github.gabrielmartins.command.core.register.CommandRegister;
import org.github.gabrielmartins.command.update.BuildInfo;
import org.github.gabrielmartins.command.update.checker.UpdateChecker;

import lombok.Getter;
import lombok.Setter;
public final class BukkitCommand extends JavaPlugin {

    @Getter @Setter
    private static BukkitCommand instance;

    @Getter @Setter
    private BuildInfo buildInfo;

    @Getter @Setter
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        this.buildInfo = BuildInfo.load();
        this.updateChecker = new UpdateChecker(buildInfo);

        new CommandRegister().create("org.github.gabrielmartins.command.impl");

        this.updateChecker.checkAutomatically();
    }

}
