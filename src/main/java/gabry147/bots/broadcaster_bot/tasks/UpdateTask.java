package gabry147.bots.broadcaster_bot.tasks;

import com.vdurmont.emoji.EmojiParser;

import gabry147.bots.broadcaster_bot.Broadcaster_bot;
import gabry147.bots.broadcaster_bot.entities.UserEntity;
import gabry147.bots.broadcaster_bot.entities.extra.ChatRole;
import gabry147.bots.broadcaster_bot.entities.extra.UserRole;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.api.objects.inlinequery.ChosenInlineQuery;
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.api.objects.inlinequery.inputmessagecontent.InputMessageContent;
import org.telegram.telegrambots.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultPhoto;
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
                    return;
                }
			}
			// save command and remove /
			String command = commandSplit[0].substring(1).toUpperCase();
			if( command.equals( PrivateCommand.COMMANDS.toString() ) ) {
				//
			}

			//user is in db
			if(userEntity != null) {
				if(userEntity.getRole().compareTo(UserRole.ACCEPTED) <= 0) {
    				//command accepted+ (custom)
    			}
    			if(userEntity.getRole().compareTo(UserRole.APPROVER) <= 0) {
    				//command approver+ (promote, forward info)
    				if(message.getForwardFrom() != null) {
        				User user = message.getForwardFrom();
        				logger.info(user);
        			}
    				if( command.equals( PrivateCommand.PROMOTE.toString() ) ) {
    					
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
}
