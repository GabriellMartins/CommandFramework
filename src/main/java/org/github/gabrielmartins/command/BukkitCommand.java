package org.github.gabrielmartins.command;

import org.bukkit.plugin.java.JavaPlugin;
import org.github.gabrielmartins.command.core.register.CommandRegister;
import lombok.Getter;
import lombok.Setter;
public final class BukkitCommand extends JavaPlugin {

    @Getter @Setter
    private static BukkitCommand instance;

    @Override
    public void onEnable() {
        BukkitCommand.setInstance(this);

        new CommandRegister().create("");

    }

}
