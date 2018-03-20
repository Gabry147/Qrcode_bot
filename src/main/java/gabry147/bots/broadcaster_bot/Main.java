package gabry147.bots.broadcaster_bot;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

public class Main {

    public static Logger logger=Logger.getLogger(Main.class);

    public static void main(String args[]){
        ApiContextInitializer.init();

        TelegramBotsApi botsApi=new TelegramBotsApi();

        Broadcaster_bot broadcasterBot=new Broadcaster_bot();

        try {
            botsApi.registerBot(broadcasterBot);
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
            logger.error("Unable to register bot");
        }
    }
}
