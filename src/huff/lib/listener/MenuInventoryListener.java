package huff.lib.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import huff.lib.events.PlayerSignInputEvent;
import huff.lib.helper.InventoryHelper;
import huff.lib.helper.ItemHelper;
import huff.lib.menuholder.MenuHolder;
import huff.lib.various.Action;

/**
 * A listener class that handles the exit behavior in menu inventories.
 * Stores the last inventories so you can go back.
 */
public class MenuInventoryListener implements Listener
{
	public MenuInventoryListener(@NotNull JavaPlugin plugin)
	{
		Validate.notNull((Object) plugin, "The plugin instance cannot be null.");
		
		this.plugin = plugin;
	}
	
	private final JavaPlugin plugin;
	
	private HashMap<UUID, List<Inventory>> lastInventories = new HashMap<>();
	private List<UUID> isExiting = new ArrayList<>();
	
	@EventHandler
	public void onInventoryMenuClick(InventoryClickEvent event)
	{		
		if (event.getClickedInventory() == null || !(event.getView().getTopInventory().getHolder() instanceof MenuHolder))
		{
			return;
		}
		
		if (ItemHelper.hasMeta(event.getCurrentItem()))
		{
			final HumanEntity human = event.getWhoClicked();
			final String currentItemName = event.getCurrentItem().getItemMeta().getDisplayName();
			
			if (currentItemName.equals(InventoryHelper.ITEM_CLOSE))
			{			
				final UUID uuid = human.getUniqueId();
						
				isExiting.add(uuid);
				lastInventories.remove(uuid);
				
				MenuHolder.close(human);
				event.setCancelled(true);
				return;
			} 
			else if (currentItemName.equals(InventoryHelper.ITEM_BACK) || currentItemName.equals(InventoryHelper.ITEM_ABORT))
			{
				isExiting.add(human.getUniqueId());
				openLastInventory(human);
				event.setCancelled(true);
				return;
			}	
		}
		
		if (((MenuHolder) event.getView().getTopInventory().getHolder()).handleClick(event))
		{
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEconomyInventoryDrag(InventoryDragEvent event)
	{	
		final InventoryHolder inventoryHolder = event.getView().getTopInventory().getHolder();

		if (inventoryHolder instanceof MenuHolder && ((MenuHolder) inventoryHolder).handleDrag(event))
		{
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onInventoryMenuClose(InventoryCloseEvent event)
	{
		final UUID uuid = event.getPlayer().getUniqueId();
		final InventoryHolder inventoryHolder = event.getInventory().getHolder();
		
		if (!(inventoryHolder instanceof MenuHolder))
		{
			return;
		}
		final MenuHolder menuHolder = (MenuHolder) inventoryHolder;

		((MenuHolder) inventoryHolder).handleClose(event);
		
		if (!menuHolder.isReturnable() && !menuHolder.isForwarding())
		{
			lastInventories.remove(uuid);
			isExiting.remove(uuid);
			return;
		}
		
		if (isExiting.contains(uuid))
		{
			isExiting.remove(uuid);
			return;
		}
	
        if (menuHolder.isReturnable() && !menuHolder.isForwarding())
		{
			openLastInventory(event.getPlayer());
			return;
		}		
		final List<Inventory> inventories = lastInventories.get(uuid);
		
		if (inventories != null)
		{
			inventories.add(event.getInventory());
		}
		else
		{
			final ArrayList<Inventory> newInventories = new ArrayList<>();
			
			newInventories.add(event.getInventory());
			lastInventories.put(uuid, newInventories);
		}
	}
	
	@EventHandler
	public void onSignMenuClose(PlayerSignInputEvent event)
	{
		if (event.isMenuSignInput())
		{
			Bukkit.getConsoleSender().sendMessage("SIGN EVENT");
			
			openLastInventory(event.getPlayer(), params -> event.getMenuHolder().handleSignInput(event));	
		}
	}
	
	private void openLastInventory(@NotNull HumanEntity human)
	{			
		openLastInventory(human, null);
	}
	
	private void openLastInventory(@NotNull HumanEntity human, @Nullable Action afterOpen)
	{			
		final List<Inventory> inventories = lastInventories.get(human.getUniqueId());
		
		if (inventories != null && !inventories.isEmpty())
		{
			final int lastIndex = inventories.size() - 1;
			
			Bukkit.getScheduler().runTask(plugin, () ->
			{				
				((MenuHolder) inventories.get(lastIndex).getHolder()).open(human, false);
				inventories.remove(lastIndex);
				
				if (afterOpen != null)
				{
					afterOpen.execute();
				}
			});
		}			
	}
}
