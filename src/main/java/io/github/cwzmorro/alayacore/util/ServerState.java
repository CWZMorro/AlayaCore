package io.github.cwzmorro.alayacore.util;

import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Stores an unsaved per-server value on the overworld, isolated between server sessions.
 */
public final class ServerState<T> {
	private final AttachmentType<T> type;

	private ServerState(AttachmentType<T> type) {
		this.type = type;
	}

	/** Creates an attachment handle under {@code id}; call once during static initialization. */
	public static <T> ServerState<T> create(Identifier id) {
		return new ServerState<>(AttachmentRegistry.create(id));
	}

	/** Returns the stored value, or null if unset or the overworld has not loaded. */
	public @Nullable T get(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		return overworld == null ? null : overworld.getAttached(this.type);
	}

	public T getOrCreate(MinecraftServer server, Function<MinecraftServer, T> create) {
		T value = this.get(server);
		if (value == null) {
			value = create.apply(server);
			this.set(server, value);
		}
		return value;
	}

	public void set(MinecraftServer server, T value) {
		server.overworld().setAttached(this.type, value);
	}

	public @Nullable T remove(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		return overworld == null ? null : overworld.removeAttached(this.type);
	}
}
