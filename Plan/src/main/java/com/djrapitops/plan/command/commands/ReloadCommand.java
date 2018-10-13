package com.djrapitops.plan.command.commands;

import com.djrapitops.plan.PlanPlugin;
import com.djrapitops.plan.system.locale.Locale;
import com.djrapitops.plan.system.locale.lang.CmdHelpLang;
import com.djrapitops.plan.system.locale.lang.CommandLang;
import com.djrapitops.plan.system.locale.lang.DeepHelpLang;
import com.djrapitops.plan.system.settings.Permissions;
import com.djrapitops.plugin.command.CommandNode;
import com.djrapitops.plugin.command.CommandType;
import com.djrapitops.plugin.command.Sender;
import com.djrapitops.plugin.logging.L;
import com.djrapitops.plugin.logging.error.ErrorHandler;
import com.djrapitops.plugin.task.AbsRunnable;
import com.djrapitops.plugin.task.RunnableFactory;

import javax.inject.Inject;

/**
 * This SubCommand is used to reload the plugin.
 *
 * @author Rsl1122
 * @since 2.0.0
 */
public class ReloadCommand extends CommandNode {

    private final PlanPlugin plugin;
    private final Locale locale;
    private final ErrorHandler errorHandler;
    private final RunnableFactory runnableFactory;

    @Inject
    public ReloadCommand(PlanPlugin plugin, Locale locale, RunnableFactory runnableFactory, ErrorHandler errorHandler) {
        super("reload", Permissions.RELOAD.getPermission(), CommandType.CONSOLE);

        this.plugin = plugin;
        this.locale = locale;
        this.runnableFactory = runnableFactory;
        this.errorHandler = errorHandler;

        setShortHelp(locale.getString(CmdHelpLang.RELOAD));
        setInDepthHelp(locale.getArray(DeepHelpLang.RELOAD));
    }

    @Override
    public void onCommand(Sender sender, String commandLabel, String[] args) {
        runnableFactory.create("Reload task", new AbsRunnable() {
            @Override
            public void run() {
                try {
                    plugin.reloadPlugin(true);
                } catch (Exception e) {
                    errorHandler.log(L.CRITICAL, this.getClass(), e);
                    sender.sendMessage(locale.getString(CommandLang.RELOAD_FAILED));
                }
                sender.sendMessage(locale.getString(CommandLang.RELOAD_COMPLETE));
            }
        }).runTaskAsynchronously();
    }
}
