package client

/**
 * A data class representing a key binding with a type and an action that configured by user in the client.
 * @param type The type key id of the key binding.
 * @param action The action id associated with the key binding.
 */
data class KeyBinding(val type: Int, val action: Int)
