package cn.drcomo.drcomoupgradeguimi;

import cn.drcomo.corelib.config.YamlUtil;
import cn.drcomo.corelib.gui.GUISessionManager;
import cn.drcomo.corelib.gui.GuiManager;
import cn.drcomo.corelib.gui.GuiActionDispatcher;
import cn.drcomo.corelib.message.MessageService;
import cn.drcomo.corelib.util.DebugUtil;
import cn.drcomo.corelib.hook.placeholder.PlaceholderAPIUtil;
import cn.drcomo.drcomoupgradeguimi.command.MainCommand;
import cn.drcomo.drcomoupgradeguimi.config.ConfigManager;
import cn.drcomo.drcomoupgradeguimi.gui.UpgradeGui;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

public final class DrcomoUpgradeGUIMI extends JavaPlugin {

    private DebugUtil logger;
    private YamlUtil yamlUtil;
    private MessageService messages;
    private GUISessionManager sessionManager;
    private GuiManager guiManager;
    private ConfigManager config;
    private UpgradeGui upgradeGui;
    private GuiActionDispatcher dispatcher;

    @Override
    public void onEnable() {
        logger = new DebugUtil(this, DebugUtil.LogLevel.INFO);
        yamlUtil = new YamlUtil(this, logger);
        yamlUtil.copyDefaults("", "");
        yamlUtil.loadConfig("config");
        yamlUtil.loadConfig("lang");
        
        PlaceholderAPIUtil papi = new PlaceholderAPIUtil(this, getName().toLowerCase());
        messages = new MessageService(this, logger, yamlUtil, papi, "lang", "");
        messages.reloadLanguages();
        
        // 在MessageService初始化后创建ConfigManager，确保能正确处理语言文件中的颜色代码
        config = new ConfigManager(yamlUtil, logger);
        config.reload();
        logger.setLevel(config.getLogLevel());

        guiManager = new GuiManager(logger);
        sessionManager = new GUISessionManager(this, logger, messages);
        dispatcher = new GuiActionDispatcher(logger, sessionManager, guiManager);
        upgradeGui = new UpgradeGui(this, guiManager, sessionManager, config, logger, dispatcher);

        getServer().getPluginManager().registerEvents(new cn.drcomo.drcomoupgradeguimi.listener.GuiClickListener(sessionManager, guiManager, config, logger, messages, dispatcher), this);
        getServer().getPluginManager().registerEvents(new cn.drcomo.drcomoupgradeguimi.listener.GuiDragListener(sessionManager, guiManager, config, logger, messages), this);
        getServer().getPluginManager().registerEvents(new cn.drcomo.drcomoupgradeguimi.listener.GuiCloseListener(sessionManager, guiManager, messages, upgradeGui), this);

        MainCommand mainCommand = new MainCommand(upgradeGui, config, messages, logger);
        getCommand("drcomoupgrade").setExecutor(mainCommand);
        getCommand("drcomoupgrade").setTabCompleter(mainCommand);
    }

    @Override
    public void onDisable() {
        if (upgradeGui != null) {
            upgradeGui.flushOnDisable();
        }
        if (sessionManager != null) {
            sessionManager.closeAllSessions();
        }
    }
}
