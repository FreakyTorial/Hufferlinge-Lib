package huff.lib.menuholder;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import huff.lib.helper.InventoryHelper;
import huff.lib.helper.ItemHelper;
import huff.lib.helper.MessageHelper;
import huff.lib.helper.StringHelper;
import huff.lib.interfaces.Action;

public class PlayerChooserHolder extends MenuHolder
{
	public static final String MENU_IDENTIFIER = "menu:playerchooser";
	
	private static final int MIN_SIZE = InventoryHelper.INV_SIZE_3;
	private static final int MAX_SIZE = InventoryHelper.INV_SIZE_6;
	private static final int START_SITE = 1;
	
	public PlayerChooserHolder(@NotNull JavaPlugin plugin, @NotNull List<UUID> players, int size, @Nullable String title, MenuExitType menuExitType, Action chooseAction)
	{
		super(MENU_IDENTIFIER, checkSize(size), title != null ? title : "§7» §9Personenauswahl", menuExitType);
		
		Validate.notNull((Object) plugin, "The plugin instance cannot be null.");
		Validate.notNull((Object) players, "The player list cannot be null.");
		Validate.notNull((Object) chooseAction, "The choose action cannot be null.");
		
		this.plugin = plugin;
		this.players = players;
		this.chooseAction = chooseAction;
		this.playersPerSite = ((this.getInventory().getSize() / InventoryHelper.ROW_LENGTH) - 2) * InventoryHelper.ROW_LENGTH - 2;
		this.maxSite = (int) Math.ceil((double) players.size() / playersPerSite);
		
		initInventory();
		setSiteFunction();
		setPlayers();
	}

	private final JavaPlugin plugin;
	private final List<UUID> players;
	private final Action chooseAction;
	private final int playersPerSite;
	private final int maxSite;
	
	private int site = START_SITE;
	
	@Override
	public boolean handleClick(@NotNull InventoryClickEvent event)
	{
		Validate.notNull((Object) event, "The inventory click event cannot be null.");
		
		final ItemStack currentItem = event.getCurrentItem();
		final int inventorySize = this.getInventory().getSize();
		
		if (ItemHelper.hasMeta(currentItem))
		{
			if (currentItem.getType() == Material.PLAYER_HEAD)
	     	{		 
	     		final OfflinePlayer currentOwningPlayer = ((SkullMeta) currentItem.getItemMeta()).getOwningPlayer();
	     		
	     		if (currentOwningPlayer != null)
	     		{
	     			chooseAction.execute(currentOwningPlayer.getUniqueId());
	     		}
	     	}
			else if (event.getSlot() == InventoryHelper.getSlotFromRowColumn(inventorySize, InventoryHelper.getLastLine(inventorySize), 4)) 
			{
				changeSite(false);
			}
			else if (event.getSlot() == InventoryHelper.getSlotFromRowColumn(inventorySize, InventoryHelper.getLastLine(inventorySize), 6))
			{
				changeSite(true);
			}
		}
     	return true;
	}
	
	private static int checkSize(int size)
	{
		if (size < MIN_SIZE)
		{
			return MIN_SIZE;
		}
		
		if (size > MAX_SIZE)
		{
			return MAX_SIZE;
		}
		return size;
	}

	private void initInventory()
	{				
		InventoryHelper.setBorder(this.getInventory(), InventoryHelper.getBorderItem());
		this.setMenuExitItem();
	}
	
	private void setSiteFunction()
	{
		final ItemStack borderItem = InventoryHelper.getBorderItem();	
		
		InventoryHelper.setItem(this.getInventory(), InventoryHelper.LAST_ROW, 5, ItemHelper.getItemWithMeta(Material.WHITE_STAINED_GLASS_PANE, 
				                                                                                             StringHelper.build("§7» Seite", MessageHelper.getHighlighted(Integer.toString(site)), "«")));
		
		if (site > START_SITE)
		{
			InventoryHelper.setItem(this.getInventory(), InventoryHelper.LAST_ROW, 4, ItemHelper.getItemWithMeta(Material.BLUE_STAINED_GLASS_PANE, "§7« §9Vorherige Seite"));
		}
		else
		{
			InventoryHelper.setItem(this.getInventory(), InventoryHelper.LAST_ROW, 4, borderItem);
		}
		
		if (site < maxSite)
		{
			InventoryHelper.setItem(this.getInventory(), InventoryHelper.LAST_ROW, 6, ItemHelper.getItemWithMeta(Material.BLUE_STAINED_GLASS_PANE, "§7» §9Nächste Seite"));
		}
		else
		{
			InventoryHelper.setItem(this.getInventory(), InventoryHelper.LAST_ROW, 6, borderItem);
		}
		final ItemStack siteItem = InventoryHelper.getItem(this.getInventory(), InventoryHelper.LAST_ROW, 5);
		
		if (siteItem != null)
		{
			siteItem.setAmount(site); 
		}
	}
	
	private void setPlayers()
	{	
		final int startIndex = (site - 1) * playersPerSite;
		final int maxIndex = startIndex + playersPerSite;
		
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> 
		{
			for (int i = startIndex; i < players.size() && i < maxIndex; i++)
			{			
				final OfflinePlayer player = Bukkit.getOfflinePlayer(players.get(i));

				this.getInventory().addItem(ItemHelper.getSkullWithMeta(player, MessageHelper.getHighlighted(player.getName(), false, false)));
			}
		});
	}

	private void clearPlayers()
	{
		InventoryHelper.setFill(this.getInventory(), null, false);
	}
	
	private void changeSite(boolean increase)
	{
		if (increase && site < maxSite)
		{
			site++;
		}
		else if (!increase && site > START_SITE)
		{
			site--;
		}	
		setSiteFunction();
		clearPlayers();
		setPlayers();
	}
}
