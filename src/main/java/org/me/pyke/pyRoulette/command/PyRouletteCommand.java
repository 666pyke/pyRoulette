package org.me.pyke.pyRoulette.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.me.pyke.pyRoulette.PyRoulette;
import org.me.pyke.pyRoulette.command.subcommands.CreateSubCommand;
import org.me.pyke.pyRoulette.command.subcommands.ListSubCommand;
import org.me.pyke.pyRoulette.command.subcommands.ReloadSubCommand;
import org.me.pyke.pyRoulette.command.subcommands.RemoveSubCommand;
import org.me.pyke.pyRoulette.economy.EconomyManager;
import org.me.pyke.pyRoulette.lang.Lang;
import org.me.pyke.pyRoulette.roulette.RouletteManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PyRouletteCommand implements CommandExecutor, TabCompleter {
    private final PyRoulette plugin;
    private final Map<String, BaseSubCommand> subCommands = new LinkedHashMap<>();

    public PyRouletteCommand(PyRoulette plugin, RouletteManager rouletteManager, EconomyManager economyManager) {
        this.plugin = plugin;
        register(new CreateSubCommand(plugin, rouletteManager));
        register(new RemoveSubCommand(plugin, rouletteManager));
        register(new ListSubCommand(plugin, rouletteManager));
        register(new ReloadSubCommand(plugin, economyManager));
    }

    private void register(BaseSubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(Locale.ROOT), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(Locale.ROOT), subCommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pyroulette.admin")) {
            Lang.send(plugin, sender, "messages.no-permission", Map.of());
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        BaseSubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            sendUsage(sender);
            return true;
        }

        try {
            subCommand.execute(sender, args);
        } catch (Exception exception) {
            Lang.send(plugin, sender, "messages.command-error", Map.of("error", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()));
            exception.printStackTrace();
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        Lang.sendList(plugin, sender, "messages.usage");
        Lang.sendRaw(sender, "", Map.of());
        Lang.sendRaw(sender, "&7&omade with <3 by 666pyke", Map.of());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pyroulette.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(primaryCommandNames(), args[0]);
        }
        BaseSubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            return List.of();
        }
        return filter(subCommand.tabComplete(sender, args), args[args.length - 1]);
    }

    private List<String> primaryCommandNames() {
        List<String> names = new ArrayList<>();
        for (BaseSubCommand subCommand : subCommands.values()) {
            if (!names.contains(subCommand.getName())) {
                names.add(subCommand.getName());
            }
        }
        return names;
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
