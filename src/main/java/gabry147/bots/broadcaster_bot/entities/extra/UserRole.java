package gabry147.bots.broadcaster_bot.entities.extra;

/**
 * The role of the user.
 * a null role corresponds to a normal user
 */
public enum UserRole {
    OWNER, //set backlog, manage admins
    ADMIN, //set commands, send broadcast, manage approver
    APPROVER, //approve/demote/ban normal player
    ACCEPTED, //use commands, receive broadcast
    NORMAL, //(same as null enum value, used only if accepted are demote) not receiving broadcast
    BANNED; //completely ignored, not appear in backlog
}
