package gabry147.bots.broadcaster_bot.tasks;

import com.vdurmont.emoji.EmojiParser;

import gabry147.bots.broadcaster_bot.Broadcaster_bot;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultPhoto;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.*;

public class UpdateTask implements Runnable {

    public static Logger logger=Logger.getLogger(UpdateTask.class);

    private final static String CHARSET="UTF-8"; // or "ISO-8859-1"

    private final static long MAX_USE_NUM=9223372036854775000L;

    private Broadcaster_bot bot;
    private Update update;

    public UpdateTask(Broadcaster_bot bot, Update update){
        this.bot=bot;
        this.update=update;
    }

    public void run() {
    	
    	if(update.hasInlineQuery()) {
    		
    		InlineQuery inlineQuery = update.getInlineQuery();
    		String query = update.getInlineQuery().getQuery();
            try {
                if (query.isEmpty()) {
                    
                } else if(query.equals("backlog")) {
                	String backlog_photo_url = "https://i.imgur.com/RsIf1mr.png";
                                        
                    InlineQueryResultPhoto backlogPhoto = new InlineQueryResultPhoto();
                    backlogPhoto.setId("backlog");
                    backlogPhoto.setPhotoUrl(backlog_photo_url);
                    backlogPhoto.setThumbUrl(backlog_photo_url);
 
                    List<InlineQueryResult> results = new ArrayList<>();
                    results.add(backlogPhoto);
                    
                	AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery();
                    answerInlineQuery.setInlineQueryId(inlineQuery.getId());
                    answerInlineQuery.setResults(results);
                    
                    bot.answerInlineQuery(answerInlineQuery);
                }
            } catch (TelegramApiException e) {
            	//TODO
            }
    	}
    }
}
