package gabry147.bots.broadcaster_bot.tasks;

import com.vdurmont.emoji.EmojiParser;

import gabry147.bots.broadcaster_bot.Broadcaster_bot;
import gabry147.bots.broadcaster_bot.entities.ChatEntity;
import gabry147.bots.broadcaster_bot.entities.UserEntity;
import gabry147.bots.broadcaster_bot.entities.extra.ChatRole;
import gabry147.bots.broadcaster_bot.entities.extra.UserRole;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.*;

public class UpdateTask implements Runnable {

    public static Logger logger=Logger.getLogger(UpdateTask.class);

    private final static String CHARSET="UTF-8"; // or "ISO-8859-1"

    private Broadcaster_bot bot;
    private Update update;

    public UpdateTask(Broadcaster_bot bot, Update update){
        this.bot=bot;
        this.update=update;
    }

    public void run() {
    	if(update.hasMessage()) {
    		Message message = update.getMessage();
    		logger.info(message);
    		long chatId = message.getChat().getId().longValue();
    		long userId = message.getFrom().getId().longValue();
    		
    		UserEntity userEntity = UserEntity.getById(userId);
    		if(userEntity != null) {
    			if(userEntity.getRole().equals(UserRole.BANNED)) {
    				return;
    			}
    		}
    		updateUserDbInfo(message.getFrom());
    		ChatEntity chatEntity = ChatEntity.getById(chatId);
    		
    		if(chatId != userId) {
    			logger.info("chat message");

    			if(update.getMessage().getNewChatMembers() != null) {
    				List<User> newUsers = update.getMessage().getNewChatMembers();
    				int botId = 0;
					try {
						botId = bot.getMe().getId();
					} catch (TelegramApiException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				for(User u : newUsers) {
    					if(u.getId() == botId) {
    						if(userEntity.getRole().compareTo(UserRole.APPROVER) <=0) {
    							sendTelegramMessage(chatId, "Ready to work!");
    						}
    						else {
    			    			LeaveChat leaveChat = new LeaveChat();
    			    			leaveChat.setChatId(chatId);
    			    			try {
    								bot.leaveChat(leaveChat);
    							} catch (TelegramApiException e) {
    								logger.error(e);
    							}
    						}
    					}  					
    					else if(chatEntity != null) {
    	    				GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
    	    				getChatAdministrators.setChatId(chatId);
    	    				List<ChatMember> admins = null;
    	    				try {
    							admins = bot.getChatAdministrators(getChatAdministrators);
    						} catch (TelegramApiException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
    	    				boolean isAdmin = false;
    	    				for(ChatMember admin : admins) {
    	    					if(admin.getUser().getId() == botId) isAdmin = true;
    	    				}
    	    				//if is not admin, no need to continue
    	    				if( ! isAdmin) return;
    	    				UserEntity entryUser = UserEntity.getById(u.getId().longValue());
    						if(chatEntity.getRole().compareTo(ChatRole.PROTECTED) <= 0) {   							
    							if(entryUser == null) {
    								kickUnapprovedUser(chatId, u.getId());
    								return;
    							}
    							else if (entryUser.getRole().compareTo(UserRole.ACCEPTED) > 0) {
    								kickUnapprovedUser(chatId, u.getId());
    								return;
    							}
    							else if (
    									(entryUser.getRole().compareTo(UserRole.ADMIN) > 0 ) &&
    									(chatEntity.getRole().compareTo(ChatRole.ADMIN) <= 0)
    									) {
    								kickUnapprovedUser(chatId, u.getId());
    								return;
    							}
    						}
    						
    					}
    				}    				
    			}
    			return;
    		}   
    		
			logger.info("private message");		
			
			String[] alphanumericalSplit = message.getText().split(" ");

			String[] commandSplit = alphanumericalSplit[0].split("@");			
			
			if(commandSplit.length>1){
                if(!commandSplit[1].equals(bot.getBotUsername())){
                	logger.info("message from another bot");
                    return;
                }
			}
			// save command and remove /
			String command = commandSplit[0].substring(1).toUpperCase();

			//user is in db
			if(userEntity != null) {
				if(userEntity.getRole().compareTo(UserRole.ACCEPTED) <= 0) {
    				//command accepted+ (custom commands)
					if( command.equals( PrivateCommand.COMMANDS.toString() ) ) {
						return;
					}
    			}
    			if(userEntity.getRole().compareTo(UserRole.APPROVER) <= 0) {
    				if( command.equals( PrivateCommand.PROMOTE.toString() ) ) {
    					String userToPromoteStringID = alphanumericalSplit[1];
    					long userToPromoteID = Long.valueOf(userToPromoteStringID);
    					UserEntity userToPromote = UserEntity.getById(userToPromoteID);
    					if(userToPromote == null) {
    						logger.info("Approved: "+userToPromoteID);
    						userToPromote = new UserEntity();
    						userToPromote.setUserId(userToPromoteID);
    						userToPromote.setRole(UserRole.ACCEPTED);   						
    					}
    					else {
    						if(userEntity.getRole().compareTo(userToPromote.getRole()) < 0) {
    							UserRole[] roles = UserRole.values();
    							UserRole previousRole = UserRole.OWNER;
    							for(UserRole currentRole : roles) {
    								if(currentRole.compareTo(userToPromote.getRole()) == 0) {
    									userToPromote.setRole(previousRole);
    									logger.info("Promote: "+userToPromoteID);
    									break;
    								}
    								previousRole = currentRole;
    							}
    						}
    					}
    					UserEntity.saveUser(userToPromote);
    					sendUserInfo(chatId, userToPromote);
    					return;
    					
    				}
    				else if( command.equals( PrivateCommand.DEMOTE.toString() ) ) {
    					String userToDemoteStringID = alphanumericalSplit[1];
    					long userToDemoteID = Long.valueOf(userToDemoteStringID);
    					UserEntity userToDemote = UserEntity.getById(userToDemoteID);
    					if(userToDemote == null) {
    						logger.info("Approved: "+userToDemoteID);
    						userToDemote = new UserEntity();
    						userToDemote.setUserId(userToDemoteID);
    						userToDemote.setRole(UserRole.BANNED);
    						sendTelegramMessage(chatId, 
    								"Unknown user, demote to BAN status. If it's an error, send:\n"
    								+ "<code>/"+PrivateCommand.PROMOTE+" "+userToDemoteID+"</code>");
    					}
    					else {
    						if(userEntity.getRole().compareTo(userToDemote.getRole()) < 0) {
    							UserRole[] roles = UserRole.values();
    							boolean previousRole = false;
    							for(UserRole role : roles) {
    								if(previousRole) {
    									userToDemote.setRole(role);
    									logger.info("Demote: "+userToDemoteID);
    									break;
    								}
    								if(role.compareTo(userToDemote.getRole()) == 0) {
    									previousRole = true;
    								}
    							}
    						}
    						else if (userEntity.getRole().compareTo(UserRole.OWNER) == 0) {
    							if(userToDemote.getRole().compareTo(UserRole.OWNER) == 0) {
    								userToDemote.setRole(UserRole.ADMIN);
									logger.info("Demote owner: "+userToDemoteID);
    							}
    							
    						}
    					}
    					UserEntity.saveUser(userToDemote);
    					sendUserInfo(chatId, userToDemote);
    					return;
    				}
    				else if( command.equals( PrivateCommand.BAN.toString() ) ) {
    					String userToBanStringID = alphanumericalSplit[1];
    					long userToBanID = Long.valueOf(userToBanStringID);
    					UserEntity userToBan = UserEntity.getById(userToBanID);
    					if(userToBan == null) {
    						logger.info("Ban: "+userToBanID);
    						userToBan = new UserEntity();
    						userToBan.setUserId(userToBanID);
    					}
    					userToBan.setRole(UserRole.BANNED);
    					UserEntity.saveUser(userToBan);
    					sendUserInfo(chatId, userToBan);
    					return;
    				}
    				//placed after command code -> approver can forward commands
    				if(message.getForwardFrom() != null) {
        				User forwardedUser = message.getForwardFrom();
        				UserEntity forwarderDBuser = UserEntity.getById(forwardedUser.getId().longValue());
        				String role = "NOT REGISTERED";
        				if(forwarderDBuser != null) role = forwarderDBuser.getRole().toString();
        				updateUserDbInfo(forwardedUser);
        				sendTelegramUserInfo(chatId, forwardedUser, role);
        				return; //with this line, it's impossible to admin to forward command
        			}
    			}
    			if(userEntity.getRole().compareTo(UserRole.ADMIN) <= 0) {
    				if( command.equals( PrivateCommand.SENDMESSAGE.toString() ) ) {
    					return;
    				}
    				//command admin+ (set custom, secure chat, members)
    			}
    			if(userEntity.getRole().compareTo(UserRole.OWNER) <= 0) {
    				//set backlog
    			} 			
    		}
    	}
    	
    }
    
    private String sanitize(String toSanitize) {
    	//replace & must be first or it will destroy all sanitizations
    	return toSanitize.replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;");
    }
    
    private void updateUserDbInfo(User user) {
    	UserEntity dbUser = UserEntity.getById(user.getId().longValue());
    	if(dbUser == null) {
    		dbUser = new UserEntity();
    		dbUser.setUserId(user.getId().longValue());
    		dbUser.setRole(UserRole.NORMAL);
    	}
		dbUser.setUsername(user.getUserName());
    	UserEntity.saveUser(dbUser);
    }
    
    private void sendTelegramMessage(long chatId, String text) {
    	SendMessage reply = new SendMessage();
		reply.setChatId(chatId);
		reply.enableHtml(true);
		reply.setText(sanitize(text));
		try {
			bot.sendMessage(reply);
		} catch (TelegramApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void kickUnapprovedUser(long chatId, int userId) {
    	KickChatMember kickChatMember = new KickChatMember();
		kickChatMember.setChatId(chatId);
		kickChatMember.setUserId(userId);
		try {
			bot.kickMember(kickChatMember);
		} catch (TelegramApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void sendTelegramUserInfo(long chatId, User user, String role) {
    	SendMessage reply = new SendMessage();
		reply.setChatId(chatId);
		reply.enableHtml(true);
		reply.setText(
				"Name: <b>" + sanitize(user.getFirstName()) + "</b>\n" +
				"Username: @" + sanitize(user.getUserName()) + "\n" +
				"<code>" + user.getId() +"</code>\n" +
				"Role: " + role + "\n\n" +
				"<code>/"+ PrivateCommand.PROMOTE +" "+user.getId()+ "</code>\n\n" +
				"<code>/"+ PrivateCommand.DEMOTE +" "+user.getId()+ "</code>\n\n" +
				"<code>/"+ PrivateCommand.BAN +" "+user.getId()+ "</code>"
				);
		try {
			bot.sendMessage(reply);
		} catch (TelegramApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void sendUserInfo(long chatId, UserEntity user) {
    	SendMessage reply = new SendMessage();
		reply.setChatId(chatId);
		reply.enableHtml(true);
		String username = "<i>NOT SAVED</i>";
		if(user.getUsername() != null) username = "@" + sanitize(user.getUsername());
		reply.setText(
				"Username: " + username + "\n" +
				"<code>" + user.getUserId() +"</code>\n" +
				"Role: " + sanitize(user.getRole().toString())
				);
		try {
			bot.sendMessage(reply);
		} catch (TelegramApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
