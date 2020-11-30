package huff.lib.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IndependencyHelper 
{
	private static final int TICK_SECOND = 20;
	
	private IndependencyHelper() { }
	
	// G E N E R A L
	
	private static @Nullable Class<?> getDependencyClass(DependencyKind dependencyKind, @NotNull String className) 
	{
		Validate.notNull((Object) className, "The class-name cannot be null.");
		
		final String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

		try 
		{
			return Class.forName(StringHelper.build(dependencyKind.getLabel(), '.', version, '.', className));
		} 
		catch (ClassNotFoundException exception) 
		{
			Bukkit.getLogger().log(Level.SEVERE, String.format("Cant get version-depent-class named \"%s\" in version \"%s\" from package \"%s\".", 
					                                           className, version, dependencyKind.getLabel()), exception);
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private static @Nullable <T extends Enum<T>> Class<T> getDependencyEnumClass(DependencyKind dependencyKind, @NotNull String enumName)
	{
		final Class<?> enumClass = getDependencyClass(dependencyKind, enumName);
		
		return enumClass != null && enumClass.isEnum() ? (Class<T>) enumClass : null;
	}
	
	public static <T extends Enum<T>> T getEnumByName(DependencyKind dependencyKind, @NotNull String enumName, @NotNull String enumValue)
	{
		final Class<T> enumClass = getDependencyEnumClass(dependencyKind, enumName);
		
		return enumClass != null ? Enum.valueOf(enumClass, enumValue) : null;
	}
	
	public static @Nullable Object createInstance(DependencyKind dependencyKind, @NotNull String packetName, @Nullable Object... params)
	{
		final Class<?> dependencyClass = getDependencyClass(dependencyKind, packetName);
				
		if (dependencyClass != null)
		{
			return createInstance(dependencyClass, params);
		}
		return null;
	}
	
	public static @Nullable Object createInstance(@Nullable Class<?> dependencyClass, @Nullable Object... params)
	{
		if (dependencyClass == null)
		{
			return null;
		}
		
		try
		{				
			List<Class<?>> paramClasses = new ArrayList<>();
			
			for (Object param : params)
			{
				paramClasses.add(param.getClass());
			}				
			return dependencyClass.getConstructor(paramClasses.toArray(new Class<?>[0])).newInstance(params);
		} 
		catch (Exception exception)
		{
			Bukkit.getLogger().log(Level.SEVERE, String.format("Cant create instance named \"%s\" with params \"%s\".", dependencyClass.getName(), Arrays.toString(params)), exception);
			return null;
		}
	}
	
	
	public static @Nullable Object invokeMethod(@Nullable Object object, @NotNull String methodName, @Nullable Object... params)
	{
		if (object == null)
		{
			return null;
		}
		
		try
		{
			List<Class<?>> paramClasses = new ArrayList<>();
			
			for (Object param : params)
			{
				paramClasses.add(param.getClass());
			}
			return object.getClass().getMethod(methodName, paramClasses.toArray(new Class<?>[0])).invoke(object, params);
		} 
		catch (Exception exception)
		{
			Bukkit.getLogger().log(Level.SEVERE, String.format("Cant invoke method named \"%s\" from object \"%s\" with params \"%s\".", methodName, object, Arrays.toString(params)), exception);
			return null;
		}
	}
	
	public static @Nullable Object invokeStaticMethod(DependencyKind dependencyKind, @NotNull String className, @NotNull String methodName, @Nullable Object... params)
	{
		final Class<?> dependencyClass = getDependencyClass(dependencyKind, className);
			
		return invokeStaticMethod(dependencyClass, methodName, params);
	}
	
	
	public static @Nullable Object invokeStaticMethod(@Nullable Class<?> dependencyClass, @NotNull String methodName, @Nullable Object... params)
	{
		if (dependencyClass == null)
		{
			return null;
		}
		
		try
		{			
			List<Class<?>> paramClasses = new ArrayList<>();
			
			for (Object param : params)
			{
				paramClasses.add(param.getClass());
			}
			return dependencyClass.getMethod(methodName, paramClasses.toArray(new Class<?>[0])).invoke(null, params);
		} 
		catch (Exception exception)
		{
			Bukkit.getLogger().log(Level.SEVERE, String.format("Cant invoke static method named \"%s\" with params \"%s\".", methodName, Arrays.toString(params)), exception);
			return null;
		}
	}
	
	// P A C K E T
	
	public static void sendPacket(@NotNull Player player, @Nullable Object packet) 
	{
		Validate.notNull((Object) player, "The player cannot be null.");
		
		if (packet == null)
		{
			return;
		}
		
		try 
		{
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object playerCon = handle.getClass().getField("playerConnection").get(handle);
			playerCon.getClass().getMethod("sendPacket", getDependencyClass(DependencyKind.NMS, "Packet")).invoke(playerCon, packet);
		} 
		catch (Exception exception) 
		{
			Bukkit.getLogger().log(Level.SEVERE, String.format("Cannot send packet to player \"%s\".", player), exception);
		}
	}
	
	public static void sendPacketDelayed(@NotNull JavaPlugin plugin, @NotNull Player player, @NotNull Object packet, long ticks) 
	{
		Validate.notNull((Object) plugin, "The plugin cannot be null.");
		Validate.notNull((Object) player, "The player cannot be null.");
		Validate.notNull(packet, "The packet-object cannot be null.");
		
		new BukkitRunnable()
		{				
			@Override
			public void run()
			{
				sendPacket(player, packet);
			}
		}.runTaskLater(plugin, ticks);
	}
	
	public static void sendPacketDelayedAsync(@NotNull JavaPlugin plugin, @NotNull Player player, @NotNull Object packet, int ticks) 
	{
		Validate.notNull((Object) plugin, "The plugin cannot be null.");
		Validate.notNull((Object) player, "The player cannot be null.");
		Validate.notNull(packet, "The packet-object cannot be null.");
		
		new BukkitRunnable()
		{				
			@Override
			public void run()
			{
				sendPacket(player, packet);
			}
		}.runTaskLaterAsynchronously(plugin, ticks);
	}
	
	// U T I L
	
	public static @NotNull String getJsonMessage(@NotNull String message)
	{
		Validate.notNull((Object) message, "The message cannot be null.");
		
		return StringHelper.build("{\"text\": \"", message, "\"}");
	}
	
	public static @NotNull Object getLocationToVec3D(@NotNull Location location)
	{
		Validate.notNull((Object) location, "The location cannot be null.");
		
		final Object vec3DObject = createInstance(DependencyKind.NMS, "Vec3D", location.getX(), location.getY(), location.getZ());
		
		if (vec3DObject != null)
		{
			return vec3DObject;
		}
		else
		{
			throw new NullPointerException(String.format("Cannot create Vec3D object from location \"%s\"", location));
		}
	}
	
	// T I M E
	
	public static long getSecondsInTicks(int seconds)
	{
		return (long) TICK_SECOND * seconds;
	}
	
	public static void sendTime(@NotNull Player player, int ticks, int infade, int outfade)
	{
		Validate.notNull((Object) player, "The player cannot be null.");
		
		final Object packet = createInstance(DependencyKind.NMS, "PacketPlayOutTitle", getEnumByName(DependencyKind.NMS, "PacketPlayOutTitle.EnumTitleAction", "TIMES"), null, infade, ticks, outfade);
		
		sendPacket(player, packet);
	}
	
	// D I S P L A Y S
	
	private static void sendRawTitle(@NotNull Player player, @NotNull String title, @NotNull String subTitle)
	{
		Validate.notNull((Object) player, "The player cannot be null.");
		Validate.notNull((Object) player, "The title cannot be null.");
		Validate.notNull((Object) player, "The subtitle cannot be null.");
		
		final Object serializedTitle = invokeStaticMethod(DependencyKind.NMS, "ChatSerializer", "a", getJsonMessage(title));
		final Object serializedSubTitle = invokeStaticMethod(DependencyKind.NMS, "ChatSerializer", "a", getJsonMessage(subTitle));
		final Object packet = createInstance(DependencyKind.NMS, "PacketPlayOutTitle", getEnumByName(DependencyKind.NMS, "PacketPlayOutTitle.EnumTitleAction", "TITLE"), serializedTitle);
		final Object packet2 = createInstance(DependencyKind.NMS, "PacketPlayOutTitle", getEnumByName(DependencyKind.NMS, "PacketPlayOutTitle.EnumTitleAction", "SUBTITLE"), serializedSubTitle);
	
		sendPacket(player, packet);
		sendPacket(player, packet2);
	}
	
	public static void sendTitle(@NotNull Player player, @NotNull String title, @NotNull String subtitle, int ticks) 
	{
		sendTitle(player, title, subtitle, ticks, TICK_SECOND, TICK_SECOND);
	}
	
	public static void sendTitle(@NotNull Player player, @NotNull String title, @NotNull String subtitle, int ticks, int infade, int outfade) 
	{
		sendRawTitle(player, title, subtitle);
		sendTime(player, ticks, infade, outfade);
	}
	
	public static void sendGameInfo(@NotNull Player player, @NotNull String message)
	{
		Validate.notNull((Object) player, "The player cannot be null.");
		Validate.notNull((Object) player, "The message cannot be null.");
		
		final Object serializedMessage = invokeStaticMethod(DependencyKind.NMS, "ChatSerializer", "a", getJsonMessage(message));
		final Object packet = createInstance(DependencyKind.NMS, "PacketPlayOutChat", serializedMessage, getEnumByName(DependencyKind.NMS, "ChatMessageType", "GAME_INFO"), player.getUniqueId());
		
		sendPacket(player, packet);
	}
	
	// I T E M

	public static @NotNull ItemStack getTaggedItemStack(@NotNull ItemStack itemStack, @NotNull String key, @NotNull String value) 
	{
		final Class<?> craftItemStackClass = getDependencyClass(DependencyKind.CRAFTBUKKIT, "inventory.CraftItemStack");
		final Object nmsItemStack = invokeStaticMethod(craftItemStackClass, "asNMSCopy", itemStack);
		final Object nmsItemCompound = (boolean) invokeMethod(nmsItemStack, "hasTag") ? invokeMethod(nmsItemStack, "getTag") : createInstance(DependencyKind.NMS, "NBTTagCompound");
		
		invokeMethod(nmsItemCompound, "setString", key, value);
		invokeMethod(nmsItemStack, "setTag", nmsItemCompound);
	
		final Object taggedItemStack = invokeStaticMethod(craftItemStackClass, "asBukkitCopy", nmsItemStack);
		
		if (taggedItemStack != null)
		{
			return (ItemStack) taggedItemStack;
		}	
		else
		{
			throw new NullPointerException(String.format("Cannot add tag \"%s\" with key \"%s\" to itemstack \"%s\"", value, key, itemStack));
		}	
	}

	public static @Nullable String getTagFromItemStack(@NotNull ItemStack itemStack, @NotNull String key)
	{
		final Class<?> craftItemStackClass = getDependencyClass(DependencyKind.CRAFTBUKKIT, "inventory.CraftItemStack");
		final Object nmsItemStack;
		
		if (itemStack.getClass().equals(craftItemStackClass))
		{		
			nmsItemStack = invokeStaticMethod(craftItemStackClass, "asNMSCopy", ItemStack.deserialize(itemStack.serialize()));
		}
		else
		{
			nmsItemStack = invokeStaticMethod(craftItemStackClass, "asNMSCopy", itemStack);
		}

		if (!(boolean) invokeMethod(nmsItemStack, "hasTag"))
		{	
			return null;		
		}	
		final Object nmsItemCompound = invokeMethod(nmsItemStack, "getTag");
		final String value = (String) invokeMethod(nmsItemCompound, "getString", key);

		return value != null ? value : null;
	}
}