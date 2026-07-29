package org.me.pyke.pyRoulette.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class BaseSubCommand {
    public abstract String getName();

    public abstract String getDescription();

    public abstract String getUsage();

    public List<String> getAliases() {
        return List.of();
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    public abstract void execute(CommandSender sender, String[] args);
}
