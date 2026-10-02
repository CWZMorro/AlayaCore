package io.github.cwzmorro.alayacore.servant;

import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * What a player chose on the selection screen.
 *
 * @param decided false until the first-join screen is answered
 * @param servant the servant, or empty for a human
 */
public record ServantChoice(boolean decided, Optional<Identifier> servant) {
	public static final ServantChoice UNDECIDED = new ServantChoice(false, Optional.empty());

	public static ServantChoice human() {
		return new ServantChoice(true, Optional.empty());
	}

	public static ServantChoice servant(Identifier servant) {
		return new ServantChoice(true, Optional.of(servant));
	}
}
