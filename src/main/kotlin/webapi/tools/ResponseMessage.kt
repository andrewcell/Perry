package webapi.tools

/**
 * Commonly used response messages.
 */
@kotlinx.serialization.Serializable
enum class ResponseMessage {
    /**
     * If request parameter, form, uri is formed unaccountable.
     */
    BAD_REQUEST,

    /**
     * If requested user don't have permission to do that. (Especially admin console, whatever)
     */
    PERMISSION_DENIED,

    /**
     * Simply not logged in.
     */
    UNAUTHORIZED,

    /**
     * Error cannot be shown to public.
     */
    INTERNAL_ERROR,

    /**
     * Everything processed properly.
     */
    SUCCESS,

    // Account Controller
    /**
     * Requested registration of a new account, but requested user's IP already in database.
     */
    ALREADY_REGISTERED_IP,

    /**
     * E-mail address already exists
     */
    ALREADY_REGISTERED_EMAIL,

    /**
     * E-mail address and password combination cannot be found in database.
     */
    INCORRECT_EMAIL_PASSWORD,
}