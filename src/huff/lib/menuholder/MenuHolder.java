package huff.lib.menuholder;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import huff.lib.events.PlayerSignInputEvent;
import huff.lib.helper.InventoryHelper;
import huff.lib.helper.SignHelper;

/**
 * A abstract class that contains the base structure for menu inventories.
 * To the base structure belongs the inventory creation, the exit behavior and a identification.
 */
public abstract class MenuHolder implements InventoryHolder 
{
	private static final String MENU_IDENT_PREFIX = "menu:";
	
	/**
	 * Creates a menu holder with the specified identifier and menu exit type holding a inventory that has the given size and title.
	 * The type is set to "CHEST" regardless of the size.
	 *
	 * @param   identifier     a in best case unique string to identify a menu holder
	 * @param   size           the size of the inventory
	 * @param   title          the title of the inventory
	 * @param   menuExitType   the wanted exit behavior
	 */
	protected MenuHolder(@NotNull String identifier, int size, @Nullable String title, MenuExitType menuExitType)
	{
		this.menuExitType = menuExitType;
		this.identifier = MENU_IDENT_PREFIX + identifier;
		this.inventory = Bukkit.createInventory(this, size, title != null ? title : "");
		resetReturnable();
		resetForwarding();
	}	
	
	/**
	 * Creates a menu holder with the specified identifier and menu exit type holding a inventory that has the given type and title.
         * The type also determines the size.
	 * 
	 * @param   identifier     a in best case unique string to identify a menu holder
	 * @param   type           the type of the inventory
	 * @param   title          the title of the inventory
	 * @param   menuExitType   the wanted exit behavior
	 */
	protected MenuHolder(@NotNull String identifier, InventoryType type, @Nullable String title, MenuExitType menuExitType)
	{
		this.menuExitType = menuExitType;
		this.identifier = MENU_IDENT_PREFIX + identifier;
		this.inventory = Bukkit.createInventory(this, type, title != null ? title : "");
		resetReturnable();
		resetForwarding();
	}
	
	private final MenuExitType menuExitType;
	private final String identifier;
	private final Inventory inventory;
	
	private boolean isReturnable;
	private boolean isForwarding;
	
	// E N T E R - E X I T
	
	/**
	 * Closes the current inventory of the given human and opens the last inventory if is stored. 
	 * 
	 * @param   human   the target human
	 */
	public static void back(@NotNull HumanEntity human)
	{
		closeInventory(human, true, false);
	}
	
	/**
	 * Closes the current inventory of the specified human and handles the full exit from a menu inventory.
	 * 
	 * @param   human   the target human
	 */
	public static void close(@NotNull HumanEntity human)
	{
		closeInventory(human, false, false);
	}
	
	private static void closeInventory(@NotNull HumanEntity human, boolean isReturnable, boolean isForwarding)
	{
		Validate.notNull((Object) human, "The human cannot be null.");
		
		final InventoryHolder inventoryHolder = human.getOpenInventory().getTopInventory().getHolder();
		
		if (inventoryHolder instanceof MenuHolder)
		{
			((MenuHolder) inventoryHolder).setReturnable(isReturnable);
			((MenuHolder) inventoryHolder).setForwarding(isForwarding);
		}
		human.closeInventory();
	}
	
	/**
	 * Closes the current inventory of the given human and opens the inventory held by this menu holder.
	 * 
	 * @param   human        the target human
	 */
	public void open(@NotNull HumanEntity human)
	{
		open(human, true);
	}
	
	/**
	 * If wanted the current inventory of the given human will be closed and the inventory held by this menu holder will be opened.
	 * 
	 * @param   human          the target human
	 * @param   withoutClose   whether the current inventory has to be closed
	 */
	public void open(@NotNull HumanEntity human, boolean withClose)
	{
		if (withClose)
		{
			closeInventory(human, false, true);
		}
		resetReturnable();
		resetForwarding();
		human.openInventory(getInventory());
	}
	
	/**
	 * Closes the current menu of the specified human and opens a new sign input.
	 * If the sign input gets closed the menu gets opened again.
	 * 
	 * @param   human   the target human
	 * @param   lines   the lines displayed on the sign
	 */
	public void openSign(@NotNull HumanEntity human, @NotNull String[] lines)
	{
		closeInventory(human, false, true);
		SignHelper.openSignInput((Player) human, lines, this);
	}
	
	// E V E N T H A N D L I N G
	
	/**
	 * Handling of the inventory click event for the menu holder the method is implemented in.
	 * 
	 * @param   event   the inventory click event from the listener
	 */
	public abstract boolean handleClick(@NotNull InventoryClickEvent event);
	
	/**
	 * Handling of the inventory drag event for the menu holder the method is implemented in.
	 * 
	 * @param   event   the inventory drag event from the listener
	 */
	public boolean handleDrag(@NotNull InventoryDragEvent event) { return false; }
	
	/**
	 * Handling of the inventory close event for the menu holder the method is implemented in.
	 * 
	 * @param   event   the inventory close event from the listener
	 */
	public void handleClose(@NotNull InventoryCloseEvent event) { }
	
	/**
	 * Handling of the sign input event for the menu holder the method is implemented in.
	 * 
	 * @param   event   the inventory close event from the listener
	 */
	public void handleSignInput(@NotNull PlayerSignInputEvent event) { }
	
	// P R O P E R T I E S
	
	/**
	 * Gets the inventory that is held by the menu holder.
	 * 
	 * @return   The inventory.
	 */
	@Override
	@NotNull
	public Inventory getInventory()
	{
		return inventory;
	}
	
	/**
	 * Gets the identifier of the menu holder.
	 * 
	 * @return   The identifier.
	 */
	@NotNull
	public String getIdentifier()
	{
		return identifier;
	}
	
	/**
	 * Checks if the identifier of the menu holder is equals to a given menu holder.
	 * 
	 * @return   The check result.
	 */
	public boolean equalsIdentifier(MenuHolder menuInventoryHolder)
	{
		return identifier.equals(menuInventoryHolder.identifier);
	}
	
	/**
	 * Gets the set menu exit behavior of the menu holder.
	 * 
	 * @return   The menu exit type.
	 */
	public MenuExitType getMenuExitType()
	{
		return menuExitType;
	}
	
	/**
	 * Indicates if it is possible to return from the menu inventory.
	 * 
	 * @return   The current returnable state.
	 */
	public boolean isReturnable()
	{
		return isReturnable;
	}
	
	/**
	 * Resets the returnable state to the default value for the menu holder.
	 */
	public void resetReturnable()
	{
		isReturnable = menuExitType.isReturnType();
	}
	
	/**
	 * Sets the returnable state to the given value.
	 * 
	 * @param   isReturnable   The wanted returnable state.
	 */
	public void setReturnable(boolean isReturnable)
	{
		this.isReturnable = isReturnable;
	}
	
	/**
	 * Indicates if the menu inventory is currently forwarding to another menu.
	 * 
	 * @return   The current forwarding state.
	 */
	public boolean isForwarding()
	{	
		return isForwarding;
	}
	
	/**
	 * Resets the forwarding state to the default value for the menu holder.
	 */
	public void resetForwarding()
	{
		isForwarding = false;
	}
	
	/**
	 * Sets the forwarding state to the given value.
	 * 
	 * @param   isReturnable   The wanted forwarding state.
	 */
	public void setForwarding(boolean isForwarding)
	{
		this.isForwarding = isForwarding;
	}

	protected void setMenuExitItem()
	{
		switch(getMenuExitType())
		{
		case CLOSE:
			InventoryHelper.setItem(getInventory(), 
					                InventoryHelper.getLastLine(getInventory().getSize()), 5, 
					                InventoryHelper.getCloseItem());
			break;
		case BACK:
			InventoryHelper.setItem(getInventory(), 
	                InventoryHelper.getLastLine(getInventory().getSize()), 9, 
	                InventoryHelper.getBackItem());
            break;
		case ABORT:
			InventoryHelper.setItem(getInventory(), 
	                InventoryHelper.getLastLine(getInventory().getSize()), 9, 
	                InventoryHelper.getAbortItem());
            break;
		default:
			break;
		}
	}
}
