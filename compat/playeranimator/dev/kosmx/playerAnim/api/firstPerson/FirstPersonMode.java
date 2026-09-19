package dev.kosmx.playerAnim.api.firstPerson;

/**
 * Compile-time downgrade shim for <b>player-animator</b>.
 *
 * <p>player-animator has no Minecraft 26.1.2 release (Modrinth tops out at 1.21.7), so this
 * directory re-declares the exact package / class / method surface that EPCA uses. It is a
 * <b>no-op</b> implementation: call sites keep compiling unchanged, but no player animation is
 * produced. See {@code PORT-STATUS.md} -> "player-animator 降级" for the per-file behaviour loss.
 *
 * <p><b>Do not ship this together with a real player-animator jar.</b> The moment an official
 * 26.1.2 build exists, delete the whole {@code compat/playeranimator} source root and drop the
 * real jar in - no call site has to change.
 */
public enum FirstPersonMode {
    FIRST_PERSON_MODEL_SPEED,
    FIRST_PERSON_MODEL,
    THIRD_PERSON_MODEL,
    DISABLED
}
