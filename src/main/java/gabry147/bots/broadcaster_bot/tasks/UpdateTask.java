package gabry147.bots.broadcaster_bot.tasks;

import com.vdurmont.emoji.EmojiParser;

import gabry147.bots.broadcaster_bot.Broadcaster_bot;
import gabry147.bots.broadcaster_bot.entities.UserEntity;
import gabry147.bots.broadcaster_bot.entities.extra.ChatRole;
import gabry147.bots.broadcaster_bot.entities.extra.UserRole;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators;
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
    		
    		if(chatId != userId) {
    			logger.info("chat message");
    			//at the moment, automatically leave chat
    			//update.getMessage().getNewChatMembers() for new members, bot included. If member is admin, bot remains
    			LeaveChat leaveChat = new LeaveChat();
    			leaveChat.setChatId(chatId);
    			try {
					bot.leaveChat(leaveChat);
				} catch (TelegramApiException e) {
					logger.error(e);
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
						//
					}
    			}
    			if(userEntity.getRole().compareTo(UserRole.APPROVER) <= 0) {
    				if(message.getForwardFrom() != null) {
        				User forwardedUser = message.getForwardFrom();
        				UserEntity forwarderDBuser = UserEntity.getById(forwardedUser.getId().longValue());
        				String role = "NOT REGISTERED";
        				if(forwarderDBuser != null) role = forwarderDBuser.getRole().toString();
        				SendMessage reply = new SendMessage();
        				reply.setChatId(chatId);
        				reply.enableHtml(true);
        				reply.setText(
        						"Name: <b>" + sanitize(forwardedUser.getFirstName()) + "</b>\n" +
        						"Username: @" + sanitize(forwardedUser.getUserName()) + "\n" +
        						"<code>" + forwardedUser.getId() +"</code>\n" +
        						"Role: " + role
        						);
        				try {
							bot.sendMessage(reply);
						} catch (TelegramApiException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        				return;
        			}
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
    					//TODO advert user
    					return;
    					
    				}
    				else if( command.equals( PrivateCommand.DEMOTE.toString() ) ) {
    					
    				}
    				else if( command.equals( PrivateCommand.BAN.toString() ) ) {
    					
    				}
    			}
    			if(userEntity.getRole().compareTo(UserRole.ADMIN) <= 0) {
    				if( command.equals( PrivateCommand.SENDMESSAGE.toString() ) ) {
    					
    				}
    				//command admin+ (set custom, secure chat, members)
    				GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
    				getChatAdministrators.setChatId(chatId);
    				try {
						bot.getChatAdministrators(getChatAdministrators);
					} catch (TelegramApiException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
}
