package gabry147.bots.broadcaster_bot.tasks;

import com.vdurmont.emoji.EmojiParser;

import gabry147.bots.broadcaster_bot.Broadcaster_bot;
import gabry147.bots.broadcaster_bot.entities.extra.ChatRole;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
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
    		logger.info(update.getMessage().getText());
    		logger.info(update.getMessage());
    		long chatId = update.getMessage().getChat().getId().longValue();
    		long userId = update.getMessage().getFrom().getId().longValue();
    		
    		if(chatId == userId) {
    			logger.info("private message");
    		}
    		else {
    			logger.info("chat message");
    			//at the moment, automatically leave chat
    			//update.getMessage().getNewChatMembers() for new members, bot included
    			LeaveChat leaveChat = new LeaveChat();
    			leaveChat.setChatId(chatId);
    			try {
					bot.leaveChat(leaveChat);
				} catch (TelegramApiException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    	
    }
}
