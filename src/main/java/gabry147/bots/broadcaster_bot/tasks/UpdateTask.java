package gabry147.bots.broadcaster_bot.tasks;

import gabry147.bots.broadcaster_bot.Broadcaster_bot;
import gabry147.bots.broadcaster_bot.entities.ChatEntity;
import gabry147.bots.broadcaster_bot.entities.CommandEntity;
import gabry147.bots.broadcaster_bot.entities.UserEntity;
import gabry147.bots.broadcaster_bot.entities.extra.ChatRole;
import gabry147.bots.broadcaster_bot.entities.extra.UserRole;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.*;
import java.util.regex.Pattern;

public class UpdateTask implements Runnable {

    public static Logger logger=Logger.getLogger(UpdateTask.class);

    private Broadcaster_bot bot;
    private Update update;

    public UpdateTask(Broadcaster_bot bot, Update update){
        this.bot=bot;
        this.update=update;
    }

    public void run() {
    	if(update.hasMessage()) {
    		Message message = update.getMessage();
    		//logger.info(message);
    		long chatId = message.getChat().getId().longValue();
    		long userId = message.getFrom().getId().longValue();
    		int botId = getBotID();
    		
    		UserEntity userEntity = UserEntity.getById(userId);
    		if(userEntity != null) {
    			if(userEntity.getRole().equals(UserRole.BANNED)) {
    				return;
    			}
    			updateUserDbInfo(message.getFrom());
    		}   		
    		ChatEntity chatEntity = ChatEntity.getById(chatId);
    		
    		if(chatId != userId) {
    			logger.info("chat message");

    			if(update.getMessage().getNewChatMembers() != null) {
    				List<User> newUsers = update.getMessage().getNewChatMembers();
    				for(User u : newUsers) {
    					if(u.getId() == botId) {
    						if(userEntity.getRole().compareTo(UserRole.ADMIN) <=0) {
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
    		    			updateChatDbInfo(message.getChat());
    		    			protectChat(chatId, botId, chatEntity, u);
    					}
    				}    				
    			} //end if newMembers
				//chat creation or media, nothing to do atm
    			if(! message.hasText()) {
    				return;
    			}
    			//if it's not a command, ignore it
    			if(! message.getText().startsWith("/")) {
    				//normal group chat message (probably bot is tagged and/or admin), nothing to do atm
    				return;
    			}
    		} //end if(chatId != userId) 
    		
			logger.info("private/command message");
			
			if(! message.hasText()) {
				//chat creation or media, nothing to do atm
				return;
			}
			
			String[] alphanumericalSplit = message.getText().split(" ");

			String[] commandSplit = alphanumericalSplit[0].split("@");			
			
			if(commandSplit.length>1 && commandSplit[0].startsWith("/")){
				String botUsername = null;
				try {
					botUsername = bot.getMe().getUserName();
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
                if(!commandSplit[1].equals(botUsername)){
                	System.out.println(commandSplit[1]);
                	try {
						System.out.println(bot.getMe().getUserName());
					} catch (TelegramApiException e) {
						logger.error("Error retriving bot itself");
						e.printStackTrace();
					}
                	logger.info("message from another bot");
                    return;
                }
			}
			// save command and remove "/"
			String command = commandSplit[0].substring(1).toUpperCase();

			if( command.equals( "START" ) ) {
				//save user that start bot
				updateUserDbInfo(message.getFrom());
				sendTelegramMessage(chatId, "Welcome! This bot help to create a protected community of chats.\n"
						+ "You must be approved to use the functionalities of this bot");
				return;
			}
			
			//user is in db
			if(userEntity != null) {
				/*
				 * Commands for ACCEPTED
				 */
				if(userEntity.getRole().compareTo(UserRole.ACCEPTED) <= 0) {
    				//command accepted+ (custom commands)
					if( command.equals( PrivateCommand.COMMANDS.toString() ) ) {
						sendCommandInfoList(chatId);
						return;
					}
					else if(CommandEntity.getById(command) != null) {
						sendTelegramHtmlMessage(chatId,
								CommandEntity.getById(command).getBody(),
								true);
					}
    			}
				/*
				 *  Commands for APPROVER
				 */
    			if(userEntity.getRole().compareTo(UserRole.APPROVER) <= 0) {
    				if( command.equals( PrivateCommand.PROMOTE.toString() ) ) {
    					String userToPromoteStringID = alphanumericalSplit[1];
    					long userToPromoteID = Long.valueOf(userToPromoteStringID);
    					promoteUser(chatId, userToPromoteID, userEntity);
    					return;
    					
    				}
    				else if( command.equals( PrivateCommand.DEMOTE.toString() ) ) {
    					String userToDemoteStringID = alphanumericalSplit[1];
    					long userToDemoteID = Long.valueOf(userToDemoteStringID);
    					demoteUser(chatId, userToDemoteID, userEntity);
    					return;
    				}
    				else if( command.equals( PrivateCommand.BAN.toString() ) ) {
    					String userToBanStringID = alphanumericalSplit[1];
    					long userToBanID = Long.valueOf(userToBanStringID);
    					banUser(chatId, userToBanID, userEntity);
    					return;
    				}
    				else if( command.equals( PrivateCommand.PIN.toString() ) ) {
    					pinMessage(chatId, botId, message);
    					return;
    				}
    			}
    			/*
    			 *  Command for ADMIN
    			 */
    			if(userEntity.getRole().compareTo(UserRole.ADMIN) <= 0) {
    				if( command.equals( PrivateCommand.SETCHAT.toString() ) ) {
    					String chatType = alphanumericalSplit[1].toUpperCase();
    					setChat(chatId, chatEntity, botId, chatType, message);
    				}
    				else if( command.equals( PrivateCommand.REMOVECHAT.toString() ) ) {
    					if(alphanumericalSplit.length == 1) {
    						removeThisChat(chatId, chatEntity);
    					}
    					else {
    						String chatToRemoveStringID = alphanumericalSplit[1];
        					long chatToRemoveID = Long.valueOf(chatToRemoveStringID);
        					removeChat(chatToRemoveID, chatId);
    					}
    					
    				}
    				else if( command.equals( PrivateCommand.CHATS.toString() ) ) {
    					List<ChatEntity> chats = ChatEntity.getAll();
    					sendChatInfoList(chatId, chats);
    				}
    				else if( command.equals( PrivateCommand.SENDMESSAGE.toString() ) ) {
    					//TODO
    					return;
    				}
    				else if( command.equals( PrivateCommand.SETCOMMAND.toString() ) ) {
    					String[] commandAndBody = message.getText().split("\n");
    					setCommand(commandAndBody, chatId, message);
    					return;
    				}
    				else if( command.equals( PrivateCommand.DELETECOMMAND.toString() ) ) {
    					if(alphanumericalSplit.length != 2 ) {
    						//TODO
    						return;
    					}
    					String commandId = alphanumericalSplit[1].toUpperCase();
    					deleteCommand(chatId, commandId);
    				}
    			}
    			/*
    			 *  Command for OWNER
    			 */
    			if(userEntity.getRole().compareTo(UserRole.OWNER) <= 0) {
    				if( command.equals( PrivateCommand.USERS.toString() ) ) {
    					List<UserEntity> members = UserEntity.getAll();
    					sendUserInfoList(chatId, members);
    				}
    				else if( command.equals( PrivateCommand.DELETEUSER.toString() ) ) {
    					String userToDeleteString = alphanumericalSplit[1];
    					long userToDeleteID = Long.valueOf(userToDeleteString);
    					deleteUser(chatId, userToDeleteID);
    					return;
    				}
    			}
    			
    			if(chatId == userId && (userEntity.getRole().compareTo(UserRole.APPROVER)) <= 0) {
    				logger.info("Private message");
    				if(message.getForwardFrom() != null) {
        				User forwardedUser = message.getForwardFrom();
        				UserEntity forwarderDBuser = UserEntity.getById(forwardedUser.getId().longValue());
        				String role = "NOT REGISTERED";
        				if(forwarderDBuser != null) role = forwarderDBuser.getRole().toString();
        				updateUserDbInfo(forwardedUser);
        				sendTelegramUserInfo(chatId, forwardedUser, role);
        				return;
        			}
    			}   			

    		} //end if userEntity != null			
    	} //end if update.hasMessage()
    } // end run

	private void protectChat(long chatId, int botId, ChatEntity chatEntity, User u) {
		if( ! botIsAdmin(chatId, botId)) {
			sendTelegramMessage(chatId, "Chat seems to be set, but bot is not admin!");
			return;
		}
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

	private String processMessageToBeStorable(Message message) {
		String textMessage = message.getText();
		String messageToForward = "";
		List<MessageEntity> entities = message.getEntities();
		MessageEntity firstCommand = entities.remove(0);
		//need a +1 to skip \n. Also, TG gives error is firstchar is \n
		int previousStop = textMessage.indexOf("\n")+1;
		for(MessageEntity me : entities) {
			int start = me.getOffset();
			int finish = me.getOffset() + me.getLength();
			//add previous not entity part
			messageToForward += textMessage.substring(previousStop, start);
			switch(me.getType()) {
				case "mention":
				case "hashtag":
				case "bot_command":
				case "url":
				case "email":
					messageToForward += sanitize(textMessage.substring(start, finish));
					break;
				case "bold": 
					messageToForward += "<b>" + sanitize(textMessage.substring(start, finish)) +"</b>"; 
					break;
				case "italic": 
					messageToForward += "<i>" + sanitize(textMessage.substring(start, finish)) +"</i>"; 
					break;
				case "code": 
					messageToForward += "<code>" + sanitize(textMessage.substring(start, finish)) +"</code>"; 
					break;
				case "pre": 
					messageToForward += "<pre>" + sanitize(textMessage.substring(start, finish)) +"</pre>"; 
					break;
				case "text_link": 
					messageToForward += "<a href='"+me.getUrl()+"'>" + sanitize(textMessage.substring(start, finish)) +"</a>"; 
					break;
				case "text_mention": 
					messageToForward += "<a href='tg://user?id="+me.getUser().getId()+"'>" + 
							sanitize(textMessage.substring(start, finish)) +"</a>"; 
					break;
			}
			previousStop=finish;
		}
		//add tail not entity
		messageToForward += textMessage.substring(previousStop);
		return messageToForward;
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
    
    private void updateChatDbInfo(Chat chat) {
    	ChatEntity dbChat = ChatEntity.getById(chat.getId().longValue());
    	if(dbChat == null) return;
		dbChat.setTitle(chat.getTitle());
    	ChatEntity.saveChat(dbChat);
    }
    
    private void sendTelegramMessage(long chatId, String text) {
    	sendTelegramHtmlMessage(chatId, text, false);
    }
    
    private void sendTelegramHtmlMessage(long chatId, String text, boolean markdown) {
    	SendMessage reply = new SendMessage();
		reply.setChatId(chatId);
		reply.enableHtml(markdown);
		reply.setText(text);
		try {
			bot.sendMessage(reply);
		} catch (TelegramApiException e) {
			logger.error("Error sending text message");
			e.printStackTrace();
		}
    }
    
    private void sendUserInfoList(long chatId, List<UserEntity> members) {
    	SendMessage reply = new SendMessage();
		reply.setChatId(chatId);
		reply.enableHtml(true);
		String text = "";
		
		for(UserEntity u : members) {
			text = text + "@"+sanitize(u.getUsername()) + "  <code>"+u.getUserId()+"</code>  "+sanitize(u.getRole().toString())+"\n";
		}
		
		reply.setText(text);		
		try {
			bot.sendMessage(reply);
		} catch (TelegramApiException e) {
			logger.error("Error sending list of users");
			e.printStackTrace();
		}
		
	}
    
    private void sendChatInfoList(long chatId, List<ChatEntity> members) {
    	SendMessage reply = new SendMessage();
		reply.setChatId(chatId);
		reply.enableHtml(true);
		String text = "<b>Chats:</b>\n";
		
		for(ChatEntity c : members) {
			text = text + "<code>"+c.getChatId()+"</code>  "
					+ sanitize(c.getRole().toString()) + "\n"
					+ "<b>Title:</b> "+sanitize(c.getTitle())
					+ "\nAdded: " + c.getAdded().toString() + "\n\n";
		}
		
		reply.setText(text);		
		try {
			bot.sendMessage(reply);
		} catch (TelegramApiException e) {
			logger.error("Error sending list of chats");
			e.printStackTrace();
		}	
    }
    
    private void sendCommandInfoList(long chatId) {
    	SendMessage reply = new SendMessage();
		reply.setChatId(chatId);
		reply.enableHtml(true);
		String text = "<b>Commands:</b>\n";
		
		List<CommandEntity> commands = CommandEntity.getAll();
		for(CommandEntity c : commands) {
			text = text + "/" +c.getCommandId().toLowerCase() + "\n";
		}
		
		reply.setText(text);		
		try {
			bot.sendMessage(reply);
		} catch (TelegramApiException e) {
			logger.error("Error sending list of commands");
			e.printStackTrace();
		}	
    }
    
    private void kickUnapprovedUser(long chatId, int userId) {
    	kickBanUnapprovedUser(chatId, userId, 60);
    }
    
    private void kickBanUnapprovedUser(long chatId, int userId, int time) {
    	KickChatMember kickChatMember = new KickChatMember();
		kickChatMember.setChatId(chatId);
		kickChatMember.setUserId(userId);
		//kick for 1 minute
		kickChatMember.setUntilDate((int)(System.currentTimeMillis()/1000)+time);
		try {
			bot.kickMember(kickChatMember);
		} catch (TelegramApiException e) {
			logger.error("Error kicking chat member");
			e.printStackTrace();
		}
    }
    
    private void banFromChats(long userId) {
    	List<ChatEntity> chats = ChatEntity.getAll();
    	for(ChatEntity c : chats) {
    		GetChatMember getChatMember = new GetChatMember();
    		getChatMember.setChatId(c.getChatId());
    		getChatMember.setUserId((int)userId);
    		ChatMember chatMember = null; //new ChatMember();
    		try {
				chatMember = bot.getChatMember(getChatMember);
			} catch (TelegramApiException e) {
				if(e instanceof TelegramApiRequestException) {
					logger.error("Ban request not accepted, maybe admin?");
				}
				else {
					e.printStackTrace();
				}
			}
    		if(chatMember != null) {
    			//ban from chat is for ever
    			kickBanUnapprovedUser(c.getChatId(), (int)userId, 5);
    		}
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
			if(e instanceof TelegramApiRequestException) {
				logger.error("User info not accepted, ???");
			}
			else {
				e.printStackTrace();
			}
		}
    }
    
    private void sendTelegramChatInfo(long chatId, ChatEntity chat) {
    	SendMessage reply = new SendMessage();
		reply.setChatId(chatId);
		reply.enableHtml(true);
		reply.setText(
				"<b>Chat ID:</b> " + chat.getChatId() + "\n" +
				"<b>Added:</b> " + chat.getAdded().toString() + "\n" +
				"<b>Role:</b> " + chat.getRole().toString() + "\n\n" +
				"For removing:\n" +
				"<code>/"+ PrivateCommand.REMOVECHAT +" "+ chat.getChatId() +"</code>"
				);
		try {
			bot.sendMessage(reply);
		} catch (TelegramApiException e) {
			if(e instanceof TelegramApiRequestException) {
				logger.error("Chat info not accepted, ???");
			}
			else {
				e.printStackTrace();
			}
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
			logger.error("Error sending db user info");
			e.printStackTrace();
		}
    }
    
    private int getBotID() {
		int botId = 0;
		try {
			botId = bot.getMe().getId();
		} catch (TelegramApiException e) {
			logger.error("Error getting bot id");
			e.printStackTrace();
		}
		return botId;
    }
    
    private boolean botIsAdmin(long chatId, int botId) {
    	GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
		getChatAdministrators.setChatId(chatId);
		List<ChatMember> admins = null;
		try {
			admins = bot.getChatAdministrators(getChatAdministrators);
		} catch (TelegramApiException e) {
			logger.error("Error checking if bot is chat admin");
			e.printStackTrace();
		}
		for(ChatMember admin : admins) {
			if(admin.getUser().getId() == botId) return true;
		}
		return false;
    }
    
    private void promoteUser(long chatId, long userToPromoteID, UserEntity senderUser) {
    	UserEntity userToPromote = UserEntity.getById(userToPromoteID);
		if(userToPromote == null) {
			logger.info("Approved: "+userToPromoteID);
			userToPromote = new UserEntity();
			userToPromote.setUserId(userToPromoteID);
			userToPromote.setRole(UserRole.ACCEPTED);   						
		}
		else {
			if(senderUser.getRole().compareTo(userToPromote.getRole()) < 0) {
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
    }
    
    private void demoteUser(long chatId, long userToDemoteID, UserEntity senderUser) {
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
			if(senderUser.getRole().compareTo(userToDemote.getRole()) < 0) {
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
			else if (senderUser.getRole().compareTo(UserRole.OWNER) == 0) {
				if(userToDemote.getRole().compareTo(UserRole.OWNER) == 0) {
					userToDemote.setRole(UserRole.ADMIN);
					logger.info("Demote owner: "+userToDemoteID);
				}
				
			}
		}
		UserEntity.saveUser(userToDemote);
		sendUserInfo(chatId, userToDemote);
    }

    private void banUser(long chatId, long userToBanID, UserEntity senderUser) {
    	UserEntity userToBan = UserEntity.getById(userToBanID);
		if(userToBan == null) {
			logger.info("Ban: "+userToBanID);
			userToBan = new UserEntity();
			userToBan.setUserId(userToBanID);
		}
		else if( senderUser.getRole().compareTo(userToBan.getRole()) > 0) {
			sendTelegramMessage(chatId, "You don't have permission");
			return;
		}
		userToBan.setRole(UserRole.BANNED);
		UserEntity.saveUser(userToBan);
		sendTelegramMessage(chatId, "Banning...");
		banFromChats(userToBan.getUserId());
		sendUserInfo(chatId, userToBan);
    }
    
    private void pinMessage(long chatId, int botId, Message message) {
    	if(! botIsAdmin(chatId, botId)) {
			sendTelegramMessage(chatId, "Bot is not chat admin");
			return;
		}
		Message messageToPin = message.getReplyToMessage();
		if(messageToPin == null) {
			sendTelegramMessage(chatId, "Nothing to pin. Please answer /"+PrivateCommand.PIN+" to the message you want to pin.");
			return;
		}
		PinChatMessage pinChatMessage = new PinChatMessage();
		pinChatMessage.setChatId(chatId);
		pinChatMessage.setMessageId(messageToPin.getMessageId());
		try {
			bot.pinChatMessage(pinChatMessage);
		} catch (TelegramApiException e) {
			logger.error("error on pin even if admin");
			e.printStackTrace();
		}
    }
    
	private void setChat(long chatId, ChatEntity chatEntity, int botId, String chatType, Message message) {
		if(chatEntity != null) {
			sendTelegramMessage(chatId, "Chat already set for: "+chatEntity.getRole().toString());
		}
		else {
			ChatRole chatRole = null;
			if(chatType.equals(ChatRole.ADMIN.toString())) {
				chatRole = ChatRole.ADMIN;
			}
			else if(chatType.equals(ChatRole.PROTECTED.toString())) {
				chatRole = ChatRole.PROTECTED;
			}
			if(chatRole != null) {
				GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
				getChatAdministrators.setChatId(chatId);
				if( ! botIsAdmin(chatId, botId)) {
					sendTelegramMessage(chatId, "Bot must be chat admin for this feature");
					return;
				}
				chatEntity = new ChatEntity();
				chatEntity.setAdded(new Date());
				chatEntity.setChatId(chatId);
				chatEntity.setTitle(message.getChat().getTitle());
				chatEntity.setRole(chatRole);
				ChatEntity.saveChat(chatEntity);
				sendTelegramChatInfo(chatId, chatEntity);
				return;
			}
			else {
				sendTelegramMessage(chatId, "Chat role not recognized");
			}
		}	
	}

	private void removeThisChat(long chatId, ChatEntity chatEntity) {
		if(chatEntity == null) {
			sendTelegramMessage(chatId, "Chat not set, nothing to remove");
		}
		else {
			sendTelegramMessage(chatId, "Chat with role: "+chatEntity.getRole().toString()+" will be removed"); 
			ChatEntity.removeChat(chatEntity);
			sendTelegramMessage(chatId, "Chat removed");    						
		}		
	}

	private void removeChat(long chatToRemoveID, long chatId) {
		ChatEntity chatToRemove = ChatEntity.getById(chatToRemoveID);
		if(chatToRemove == null) {
			sendTelegramMessage(chatId, "Chat ID not found, nothing to remove");
		}
		else {
			sendTelegramMessage(chatId, "Chat "+ chatToRemove.getTitle() +" with role: "+chatToRemove.getRole().toString()+" will be removed"); 
			sendTelegramMessage(chatToRemove.getChatId(), "Chat protection will be disabled");
			ChatEntity.removeChat(chatToRemove);
			sendTelegramMessage(chatId, "Chat removed");  
		}		
	}

	private void setCommand(String[] commandAndBody, long chatId, Message message) {
		if(commandAndBody.length < 2) {
			//TODO
			logger.info("setcommand error, \\n not found");
			return;
		}
		String[] setAndCommandId = commandAndBody[0].split(" ");
		if(setAndCommandId.length != 2) {
			//TODO
			logger.info("setcommand error, commandId not found");
			return;
		}
		String commandId = setAndCommandId[1].toUpperCase();
		Pattern p = Pattern.compile("[^a-zA-Z0-9]");
		if(p.matcher(commandId).find()) {
			//TODO
			logger.info("setcommand error, commandId not alphanumerical");
			return;
		}
		for(PrivateCommand pc : PrivateCommand.values()) {
			if((commandId.toUpperCase()) == pc.toString()) {
				//TODO
				logger.info("setcommand error, reserved command");
				return;
			}
		}
		
		CommandEntity proposedCommand = CommandEntity.getById(commandId);
		if(proposedCommand == null) {
			proposedCommand = new CommandEntity();
			proposedCommand.setCommandId(commandId);
		}
		else {
			sendTelegramHtmlMessage(chatId,
					"<b>Command already set, previous body:</b>\n" + proposedCommand.getBody(), 
					true);
		}
		proposedCommand.setBody(
				processMessageToBeStorable(message)
				);
		CommandEntity.saveCommand(proposedCommand);
		sendTelegramMessage(chatId, "Command /"+proposedCommand.getCommandId().toLowerCase()+" set!");
	}
	
	private void deleteCommand(long chatId, String commandId) {
		CommandEntity commandToDelete = CommandEntity.getById(commandId);
		if(commandToDelete != null) {
			CommandEntity.deleteCommand(commandToDelete);
			sendTelegramMessage(chatId, "Command deleted!");
		}
		else {
			sendTelegramMessage(chatId, "Nothing to delete!");
		}	
	}

	private void deleteUser(long chatId, long userToDeleteID) {
		UserEntity userToDelete = UserEntity.getById(userToDeleteID);
		if(userToDelete == null) {
			sendTelegramMessage(chatId, "No user to delete");
		}
		else {
			sendUserInfo(chatId, userToDelete);
			UserEntity.deleteUser(userToDelete);
			sendTelegramMessage(chatId, "Deleted!");
		}	
	}
}
