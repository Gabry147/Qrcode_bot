package gabry147.bots.broadcaster_bot.tasks;

import com.vdurmont.emoji.EmojiParser;

import gabry147.bots.broadcaster_bot.Broadcaster_bot;
import gabry147.bots.broadcaster_bot.entities.ChatEntity;
import gabry147.bots.broadcaster_bot.entities.User;
import gabry147.bots.broadcaster_bot.entities.extra.Role;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.*;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.api.objects.inlinequery.result.chached.InlineQueryResultCachedPhoto;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class UpdateTask implements Runnable {

    public static Logger logger=Logger.getLogger(UpdateTask.class);

    private final static String CHARSET="UTF-8"; // or "ISO-8859-1"

    private final static long MAX_USE_NUM=9223372036854775000L;

    private Broadcaster_bot bot;
    private Update update;

    private final static boolean VCARD_ENABLED=true;

    public UpdateTask(Broadcaster_bot bot, Update update){
        this.bot=bot;
        this.update=update;
    }

    public void run() {

        if(update.hasMessage() && update.getMessage().hasText()){
            SendMessage message = new SendMessage() // Create a SendMessage object with mandatory fields
                    .setChatId(update.getMessage().getChatId());

            if(logger.isInfoEnabled()) {
                logger.info("user name: "+update.getMessage().getFrom().getUserName());
                logger.info("user id:"+update.getMessage().getFrom().getId());
                logger.info("message id:"+update.getMessage().getMessageId());
                logger.info(update.getMessage().getText());
            }

            updateChatAndUserInformation();

            String[] splits=update.getMessage().getText().split(" ");

            String[] commandSplit=splits[0].split("@");

            if(commandSplit.length>1){
                if(!commandSplit[1].equals(bot.getBotUsername())){
                    return;
                }
            }

            switch (commandSplit[0]){
                case "/start":
                    //break;
                case "/instruction":
                case "/help":
                    sendHelpMessage();
                    break;
                case "/chats":

                    User u=User.getById(Long.valueOf(update.getMessage().getFrom().getId()));

                    if(u==null){
                        return;
                    }

                    if(u.getRole()==null){
                        return;
                    }

                    if(!u.getRole().equals(Role.ADMIN)){
                        return;
                    }

                    List<ChatEntity> chatEntities=ChatEntity.getAllByDate();
                    String chatsString="Last used chat\n";

                    Iterator<ChatEntity> it=chatEntities.iterator();

                    while (it.hasNext()){
                        ChatEntity c=it.next();

                        Iterator<User> it1=c.getUsers().iterator();
                        while (it1.hasNext()){
                            User user=it1.next();
                            chatsString+=user.getUsername()+"\t*"+c.getNumberOfUses()+"*\t"+c.getLasUse()+"\t"+
                                    c.getChatId()+"\n";
                        }
                    }

                    message.setText(chatsString.replace("_","\\_"));
                    message.enableMarkdown(true);

                    bot.sendResponse(message);

                    return;

                default:
                    if(update.getMessage().isUserMessage()) {
                        message.setText("Type /help for the list of available commands");
                        bot.sendResponse(message);
                    }
                    break;
            }


        }
        else if(update.hasMessage() && update.getMessage().hasPhoto()){
            List<PhotoSize> photoSizes=update.getMessage().getPhoto();

            logger.info("user name: "+update.getMessage().getFrom().getUserName());
            logger.info("user id:"+update.getMessage().getFrom().getId());
            logger.info("photo");

            updateChatAndUserInformation();

            if(photoSizes.size()<=0){

                String text="Unable to receive the file";

                sendErrorMessage(text);

                return;
            }

            String f_id = photoSizes.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null).getFileId();

            //logger.info("file size <PhotoSize>: "+max);

            GetFile getFileRequest = new GetFile();
            getFileRequest.setFileId(f_id);

            File file=null;

            try {
                file=bot.getFile(getFileRequest);
            } catch (TelegramApiException e) {
                logger.error("file not exits");
                sendErrorMessage("File not exits");
                e.printStackTrace();
            }

            logger.info("file url: "+file.getFileUrl(bot.getBotToken()));
            logger.info(file.toString());

            InputStream is=null;

            try {
                is=getFileInputStream(file);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                sendErrorMessage("Unable to download file");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                sendErrorMessage("Unable to download file");
                return;
            }
        }
        else if(update.hasInlineQuery() && bot.dbLoggingEnabled()){
            /*the inline query mode is available only with the dblogging enable*/
            logger.info("Inline query");

            if(!update.getInlineQuery().hasQuery())
                return;

            logger.info("User name "+update.getInlineQuery().getFrom().getUserName());
            logger.info("Query: "+update.getInlineQuery().getQuery());

            User u=User.getById(Long.valueOf(update.getInlineQuery().getFrom().getId()));

            if(u==null){
                u=new User();
                u.setUserId(Long.valueOf(update.getInlineQuery().getFrom().getId()));
                u.setUsername(update.getInlineQuery().getFrom().getUserName());
            }

            AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery();
            answerInlineQuery.setInlineQueryId(update.getInlineQuery().getId());

            List<InlineQueryResult> inlineQueryResults = new ArrayList<>();
            answerInlineQuery.setResults(inlineQueryResults);

            try {
                answerInlineQuery.setCacheTime(0);
                logger.info("send result= " + bot.answerInlineQuery(answerInlineQuery));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        else if(update.hasMessage() && update.getMessage().getContact()!=null){

            /* Vcart example

            BEGIN:VCARD
            VERSION:3.0
            FN:Paolo Rossi
            ADR:;;123 Street;City;Region;PostalCode;Country
            TEL:+908888888888
            TEL:+901111111111
            TEL:+902222222222
            EMAIL;TYPE=home:homeemail@example.com
            EMAIL;TYPE=work:workemail@example.com
            URL:http://www.google.com
            END:VCARD
             */

            updateChatAndUserInformation();

            Contact contact=update.getMessage().getContact();

            String text="BEGIN:VCARD\n" +
                    "VERSION:3.0\n" +
                    "FN:"+(contact.getFirstName()!=null?contact.getFirstName():"")+" "+
                    (contact.getLastName()!=null?contact.getLastName():"")+"\n" +
                    "TEL:"+(contact.getPhoneNumber()!=null?contact.getPhoneNumber():"")+"\n" +
                    "END:VCARD";
        }
    }

    private boolean decodeAndSendLocation(String text) {
        String temp=text.toLowerCase().replace("geo:","");
        String coordSplit[]=temp.split(",");

        SendLocation sendLocation=new SendLocation();
        sendLocation.setChatId(update.getMessage().getChatId());

        try {
            if(coordSplit.length==4){
                sendLocation.setLatitude(Float.parseFloat(coordSplit[0]+","+coordSplit[1]));
                sendLocation.setLongitude(Float.parseFloat(coordSplit[2]+","+coordSplit[3]));
            }
            else if(coordSplit.length==2){
                sendLocation.setLatitude(Float.parseFloat(coordSplit[0]));
                sendLocation.setLongitude(Float.parseFloat(coordSplit[1]));
            }
            else {
                sendErrorMessage(text);
                logger.error("Unable to generate the location for: "+text);
                return true;
            }
        }
        catch (Exception e){
            sendErrorMessage(text);
            logger.error("Unable to generate the location for: "+text);
            return true;
        }

        try {
            bot.sendLocation(sendLocation);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendErrorMessage("Unable to send the location");
            return true;
        }
        return false;
    }

    private boolean decodeAndSendContact(SendMessage message, String text) {
        if(logger.isInfoEnabled()){
            logger.info("decoding contact");
        }

        SendContact sendContact=new SendContact();
        sendContact.setChatId(update.getMessage().getChatId());

                    /*retrieve first and last name*/
        int ind=text.indexOf("FN:")+3;

        if(ind!=-1) {
            String temp = text.substring(ind, text.indexOf('\n', ind));

            String lastAndFirstName[] = temp.split(" ");

            sendContact.setFirstName(lastAndFirstName[0]);
            if(lastAndFirstName.length>1){
                sendContact.setLastName(lastAndFirstName[1]);
            }
        }

                    /*if vcard is enabled, send also vcard file*/
        if(VCARD_ENABLED)
            sendVcardFile(text,sendContact.getFirstName());

        ind=text.indexOf("TEL:")+4;

        if(ind!=-1){
            sendContact.setPhoneNumber(text.substring(ind,text.indexOf('\n',ind)));
        }

        try {
            bot.sendContact(sendContact);

            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            message.setText(text);
        }
        return false;
    }

    private void sendVcardFile(String text,String name) {
        SendDocument sendDocument=new SendDocument();
        sendDocument.setChatId(update.getMessage().getChatId());


        InputStream is=new ByteArrayInputStream(text.getBytes());

        if(is==null)
            return;

        String documetName="vCard";

        if(name!=null)
            documetName=name;

        sendDocument.setNewDocument(documetName+".vcard",is);

        try {
            bot.sendDocument(sendDocument);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    private void sendHelpMessage() {
        String helpText="- Write /encode <text>: the bot will encode the <text> inside a QRCode!\n"+
                "- Write /encode_wifi <SSID> <WPA|WEP> <password>: the bot will encode the wifi credentials!\n" +
                "- Write /encode_wifi <SSID> <password> for WPA network!\n";

        String text2="- Click :paperclip: and send a *photo* with a QRCode: the bot will decode it!\n" +
                "- Send a *Location* or a *Contact*: the bot will encode it in a QRCode!\n" +
                "- Use the bot also in chats: write @"+bot.getBotUsername().replace("_","\\_")+" <text> to send " +
                "your friends a QRCoded message! (inline mode)";

        String text3="- Write /help or /instruction to see the commands again! :yum:";

        SendMessage message=new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        //message.enableMarkdown(true);
        message.setText(EmojiParser.parseToUnicode(helpText));



        SendMessage message1=new SendMessage();
        message1.setChatId(update.getMessage().getChatId());
        message1.enableMarkdown(true);
        message1.setText(EmojiParser.parseToUnicode(text2));

        SendMessage message2=new SendMessage();
        message2.setChatId(update.getMessage().getChatId());
        message2.enableMarkdown(true);
        message2.setText(EmojiParser.parseToUnicode(text3));

        bot.sendResponse(message1);
        bot.sendResponse(message);
        bot.sendResponse(message2);

        SendMessage enjoyMessage=new SendMessage();
        enjoyMessage.setChatId(update.getMessage().getChatId());
        enjoyMessage.setText(EmojiParser.parseToUnicode("Enjoy! :grin:"));

        bot.sendResponse(enjoyMessage);

        /*SendPhoto sendPhoto=new SendPhoto();
        sendPhoto.setChatId(update.getMessage().getChatId());

        sendPhoto.setNewPhoto("sendQr",getClass().getClassLoader().getResourceAsStream("img/sendQr.png"));

        try {
            qrCodeBot.sendPhoto(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            logger.error("Unable to send the photo");
        }*/
    }

    private InputStream getInputStreamFromBufferedImage(BufferedImage bufferedImage) {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();

        try {
            ImageIO.write(bufferedImage,"jpg",baos);
            baos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(baos.toByteArray());
    }

    private void updateChatAndUserInformation(){
        updateChatAndUserInformation(update.getMessage().getChatId(),Long.valueOf(update.getMessage().getFrom().getId()),
                update.getMessage().getFrom().getUserName());
    }

    private void updateChatAndUserInformation(Long chatId,Long userId,String username) {

        if(!bot.dbLoggingEnabled())
            return;

        ChatEntity c=ChatEntity.getById(chatId);

        if(c==null){
            c=new ChatEntity();
            c.setChatId(chatId);
        }

        c.setLasUse(new Date(System.currentTimeMillis()));

        if(c.getNumberOfUses()==null){
            c.setNumberOfUses(0L);
        }

        if(c.getNumberOfUses()<MAX_USE_NUM)
            c.setNumberOfUses(c.getNumberOfUses()+1);

        User u=User.getById(userId);

        if(u==null){
            u=new User();
            u.setUserId(userId);
        }

        u.setUsername(username);

        u=User.saveUser(u);

        if(c.getUsers()==null){
            c.setUsers(new HashSet<>());
        }

        c.getUsers().add(u);

        u=User.saveUser(u);
        c=ChatEntity.saveChat(c);
    }


    private InputStream getFileInputStream(File file) throws IOException {
        URL url=new URL(file.getFileUrl(bot.getBotToken()));
        InputStream is=url.openStream();

        //Files.copy(is,Paths.get("download.jpg"));
        return is;
    }

    private void sendErrorMessage(String text) {
        SendMessage message=new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText(text);
        bot.sendResponse(message);
    }
}
