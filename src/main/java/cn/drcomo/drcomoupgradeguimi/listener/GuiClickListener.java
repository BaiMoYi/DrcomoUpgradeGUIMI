package cn.drcomo.drcomoupgradeguimi.listener;

import cn.drcomo.corelib.gui.GUISessionManager;
import cn.drcomo.corelib.gui.GuiManager;
import cn.drcomo.corelib.util.DebugUtil;
import cn.drcomo.drcomoupgradeguimi.config.ConfigManager;
import cn.drcomo.corelib.message.MessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import cn.drcomo.drcomoupgradeguimi.util.ItemWhitelistUtil;
import cn.drcomo.corelib.gui.GuiActionDispatcher;
import cn.drcomo.corelib.gui.ClickContext;

/**
 * Handle click events inside upgrade GUI.
 */
public class GuiClickListener implements Listener {
    private final GUISessionManager sessionManager;
    private final GuiManager guiManager;
    private final ConfigManager config;
    private final DebugUtil logger;
    private final MessageService messages;
    private final GuiActionDispatcher dispatcher;

    public GuiClickListener(GUISessionManager sessionManager, GuiManager guiManager,
                            ConfigManager config, DebugUtil logger, MessageService messages,
                            GuiActionDispatcher dispatcher) {
        this.sessionManager = sessionManager;
        this.guiManager = guiManager;
        this.config = config;
        this.logger = logger;
        this.messages = messages;
        this.dispatcher = dispatcher;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory guiInv = e.getView().getTopInventory();
        if (!sessionManager.validateSessionInventory(player, guiInv)) {
            return;
        }
        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null) {
            return;
        }

        boolean inGuiArea = clickedInv == guiInv;
        ConfigManager.GuiConfig guiCfg = null;
        if (inGuiArea) {
            guiCfg = config.getGuiConfig();
            int rawSlot = e.getRawSlot();
            if (rawSlot == guiCfg.closeButtonSlot() || guiCfg.decorativeSlots().contains(rawSlot)) {
                e.setCancelled(true);
                if (rawSlot == guiCfg.closeButtonSlot()) {
                    // 检查玩家手上是否有物品，如果有则返回到背包中
                    ItemStack cursorItem = player.getItemOnCursor();
                    if (cursorItem != null && !cursorItem.getType().isAir()) {
                        // 先清空玩家手上的物品，再返回到背包中
                        player.setItemOnCursor(null);
                        
                        if (player.getInventory().addItem(cursorItem).size() > 0) {
                            // 如果背包已满，则掉落在地上
                            player.getWorld().dropItem(player.getLocation(), cursorItem);
                            messages.send(player, "item-returned-dropped");
                        } else {
                            messages.send(player, "item-returned");
                        }
                    }
                    // 处理完物品后再调用回调
                    ClickContext ctx = ClickContext.from(e, sessionManager);
                    dispatcher.handleClick(ctx, e);
                }
                return; // 已处理完毕或被拦截
            }
        }

        // ===== 玩家背包中的 Shift 点击 / 数字键交换 等危险操作 =====
        if (clickedInv != guiInv) {
            ClickContext ctx = ClickContext.from(e, sessionManager);
            if (e.isShiftClick() || guiManager.isDangerousClick(ctx)) {
                ItemStack moved = e.getCurrentItem();
                if (moved != null && !moved.getType().isAir() && !ItemWhitelistUtil.isWhitelisted(moved, config, logger)) {
                    e.setCancelled(true);
                    
                    messages.send(player, "invalid-item");
                }
            }
            return; // 不继续处理 GUI 内槽位逻辑
        }

        // ===== 点击 GUI 内部槽位 =====
        e.setCancelled(true); // 先拦截，后续条件放行
        ConfigManager.GuiConfig gui = guiCfg != null ? guiCfg : config.getGuiConfig();
        int slot = e.getRawSlot();
        if (slot == gui.closeButtonSlot()) {
            player.closeInventory();
            return;
        }
        if (gui.decorativeSlots().contains(slot)) {
            return;
        }
        ItemStack current;
        switch (e.getClick()) {
            case NUMBER_KEY -> {
                current = player.getInventory().getItem(e.getHotbarButton());
            }
            case SWAP_OFFHAND -> {
                current = player.getInventory().getItemInOffHand();
            }
            default -> {
                current = e.getCursor();
            }
        }
        if (ItemWhitelistUtil.isWhitelisted(current, config, logger)) {
            e.setCancelled(false);
        } else {
            messages.send(player, "invalid-item");
        }
    }

}
