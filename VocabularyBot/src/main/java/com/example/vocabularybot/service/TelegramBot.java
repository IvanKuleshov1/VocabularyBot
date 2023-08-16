package com.example.vocabularybot.service;

import com.example.vocabularybot.model.User;
import com.example.vocabularybot.model.UserRepository;
import com.example.vocabularybot.model.Words;
import com.example.vocabularybot.model.WordsRepository;
import com.vdurmont.emoji.EmojiParser;

import com.example.vocabularybot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import org.telegram.telegrambots.meta.api.objects.InputFile;

import static java.lang.Math.toIntExact;

import java.io.File;
import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {


    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WordsRepository wordsRepository;
    final BotConfig config;

    private final String rightButtonemj = EmojiParser.parseToUnicode(":arrow_right:");
    private final String leftButtonemj = EmojiParser.parseToUnicode(":arrow_left:");
    private final String backToMenu = EmojiParser.parseToUnicode(":back:");
    static final String HELP_TEXT = "Информация о командах: \n\n" +
            "/start "+ "регистрация и получение стартового сообщения\n\n" +
            "/test "+ "пройти тестирование по изученным словам\n\n" +
            "/newbunch "+ "перейти к изучению слов\n\n" +
            "/deletemydata "+ "обнулить мои ответы\n\n" +
            "/help "+ "информация о командах\n\n" +
            "/faq "+ "ответы на часто задаваемые вопросы\n\n";


    static final String FAQ = "*Vocabulary bot FAQ*\n" +
            "\n" +
            "\n" +
            "Простой способ выучить *5000 важных английских слов*\n" +
            "Любое слово легко запомнить в контексте – когда оно связано с ситуацией или образом. Поэтому новые слова родного языка мы запоминаем практически сразу, а иностранные – учим с трудом.\n" +
            "Телеграм-бот *vocabulary* - это простой способ быстро прокачать словарный запас до уверенного *Upper-Intermediate (B2)*. Он содержит 5000 часто употребляемых английских слов с картинками, озвучкой и примерами использования.\n" +
            "\n" +
            "*FAQ*\n" +
            "\n" +
            "- *Почему именно эти слова?*\n" +
            "За основу взят частотный рейтинг употреблений слов, полученный путём анализа большого массива текстов. Отобраны слова, встречающиеся не менее 3 раз на 1000 страниц. Лёгкие общеизвестные слова (drink, green, man) и понятные без перевода интернациональные термины (gangster, effect и т. д.) не вошли в основную часть, но сохранены в расширенном списке - о нём ниже. Добавлены около 400 популярных в разговорном английском фразовых глаголов (take off, make up и др.) Разделены слова с несколькими значениями (pretty, coach и др.)\n" +
            "\n" +
            "- *Что даст прохождение курса?*\n" +
            "Словарный запас для уверенного [устного общения](https://www.fluentu.com/blog/how-many-words-do-i-need-to-know/)  и [чтения лёгкой литературы и публицистики](https://scholarspace.manoa.hawaii.edu/server/api/core/bitstreams/04d7edf5-be1c-4a1e-9c91-995135ac4120/content). От вас требуется только изучить одну книжку с картинками.\n" +
            "\n" +
            "- *Как это работает?*\n" +
            "Очень просто: листаете карточки, отмечаете звёздочкой незнакомые слова, а затем повторяете их через увеличивающиеся интервалы времени. После нескольких повторений слово неизбежно запоминается.\n" +
            "\n" +
            "- *Откуда примеры?*\n" +
            "В основе реальные случаи употребления из литературы и публицистики. Кроме слов, которые удобно показать, относящихся к еде, спорту и т. п., словарь содержит и функциональные слова - \"причинять\", \"кстати\" и т. д. В этом случае иллюстрируется пример использования. Такие слова также легко запоминаются в контексте.\n" +
            "\n" +
            "- *Как расположены слова?*\n" +
            "В первой половине *(2500)* самые употребительные слова, во второй – более редкие. Внутри каждой из половин слова перемешаны.";


    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Регистрация и получение стартового сообщения."));
        listOfCommands.add(new BotCommand("/test", "Пройти тестирование по изученным словам."));
        listOfCommands.add(new BotCommand("/deletemydata", "Обнулить мои ответы."));
        listOfCommands.add(new BotCommand("/newbunch", "Перейти к изучению слов."));
        listOfCommands.add(new BotCommand("/help", "Информация о командах."));
        listOfCommands.add(new BotCommand("/faq", "Ответы на часто задаваемые вопросы."));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));

        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }

    }


    public SendMessage sendInlineKeyBoardMessage(long chatId) {
        SendMessage message = new SendMessage(String.valueOf(chatId), "mydata command");

        sendImageUploadingAFile(String.valueOf(chatId));


        message.setReplyMarkup(menuTest());
        return message;
    }


    public void sendImageUploadingAFile(String chatId) {
        Optional<User> user = userRepository.findById(Long.valueOf(chatId));
        Long recent_word = Long.valueOf(user.get().getRecentWord());

        Optional<Words> wordDataFromDB = wordsRepository.findById(recent_word);
        Words word = wordDataFromDB.get();


        // Create send method
        SendPhoto sendPhotoRequest = new SendPhoto();


        sendPhotoRequest.setCaption(word.getEngword() + " - " + word.getTranslation() + '\n' + word.getExample());

        sendPhotoRequest.setReplyMarkup(menuBunches());


        // Set destination chat id
        sendPhotoRequest.setChatId(chatId);
        // Set the photo file as a new photo (You can also use InputStream with a constructor overload)
//        sendPhotoRequest.setPhoto(new InputFile(new File(filePath)));
        sendPhotoRequest.setPhoto(new InputFile(new File(word.getImagePath())));
        try {
            // Execute the method
            execute(sendPhotoRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    private void registerUser(Message msg) {

        if (userRepository.findById(msg.getChatId()).isEmpty()) {

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(user.getUserName());
//            user.setRegisteredAt(new TimeStamp(System.currentTimeMillis()));
            user.setRecentWord(1);
            user.setRightAnswers(0);
            user.setAnswers(0);

            userRepository.save(user);
            log.info("user saved: " + user);
        }

    }

    public InlineKeyboardMarkup menuTest() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonLeft = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonMain = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonRight = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonA = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonB = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonC = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonD = new InlineKeyboardButton();

        inlineKeyboardButtonRight.setText(rightButtonemj);
        inlineKeyboardButtonRight.setCallbackData("right button");

        inlineKeyboardButtonLeft.setText(leftButtonemj);
        inlineKeyboardButtonLeft.setCallbackData("i do something after click on left_button");

        inlineKeyboardButtonA.setText("A");
        inlineKeyboardButtonB.setText("B");
        inlineKeyboardButtonC.setText("C");
        inlineKeyboardButtonD.setText("D");
        inlineKeyboardButtonA.setCallbackData("a");
        inlineKeyboardButtonB.setCallbackData("b");
        inlineKeyboardButtonC.setCallbackData("c");
        inlineKeyboardButtonD.setCallbackData("d");

        inlineKeyboardButtonMain.setText("Main button");
        inlineKeyboardButtonMain.setCallbackData("main button");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow4 = new ArrayList<>();

        keyboardButtonsRow1.add(inlineKeyboardButtonMain);

        keyboardButtonsRow2.add(inlineKeyboardButtonA);
        keyboardButtonsRow2.add(inlineKeyboardButtonB);

        keyboardButtonsRow3.add(inlineKeyboardButtonC);
        keyboardButtonsRow3.add(inlineKeyboardButtonD);

        keyboardButtonsRow4.add(inlineKeyboardButtonLeft);
        keyboardButtonsRow4.add(inlineKeyboardButtonRight);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);
        rowList.add(keyboardButtonsRow2);
        rowList.add(keyboardButtonsRow3);
        rowList.add(keyboardButtonsRow4);
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup menuBunches() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonLeft = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonRight = new InlineKeyboardButton();

        inlineKeyboardButtonRight.setText("Next" + rightButtonemj);
        inlineKeyboardButtonRight.setCallbackData("next word");
        inlineKeyboardButtonLeft.setText(leftButtonemj + "Previous");
        inlineKeyboardButtonLeft.setCallbackData("previous word");
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonLeft);
        keyboardButtonsRow1.add(inlineKeyboardButtonRight);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);
        rowList.add(backToMainMenuButton());
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }

    public List<InlineKeyboardButton> backToMainMenuButton(){
        InlineKeyboardButton inlineKeyboardButtonMain = new InlineKeyboardButton();
        inlineKeyboardButtonMain.setText(backToMenu+ "Back to menu");
        inlineKeyboardButtonMain.setCallbackData("back to menu");
        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow2.add(inlineKeyboardButtonMain);
        return keyboardButtonsRow2;
    }

    public void iterBunch(long chat_id, long message_id, boolean direction) {

        EditMessageCaption new_message = new EditMessageCaption();


        Optional<User> user = userRepository.findById(chat_id);
        if (direction) user.get().setRecentWord(user.get().getRecentWord() + 1);
        else if (!direction && user.get().getRecentWord() > 1) user.get().setRecentWord(user.get().getRecentWord() - 1);

        userRepository.save(user.get());
        new_message.setChatId(chat_id);
        new_message.setMessageId(toIntExact(message_id));

        Long recentWordId = Long.valueOf(Objects.requireNonNull(userRepository.findById(chat_id).orElse(null)).getRecentWord());
        String filePath = Objects.requireNonNull(wordsRepository.findById(recentWordId).orElse(null)).getImagePath();
        String word = Objects.requireNonNull(wordsRepository.findById(recentWordId).orElse(null)).getEngword();
        String translation = Objects.requireNonNull(wordsRepository.findById(recentWordId).orElse(null)).getTranslation();
        String caption = Objects.requireNonNull(wordsRepository.findById(recentWordId).orElse(null)).getExample();

        EditMessageMedia new_media = new EditMessageMedia();
        new_media.setChatId(chat_id);
        new_media.setMessageId(toIntExact(message_id));
        InputMediaPhoto newPhoto = new InputMediaPhoto();
        newPhoto.setMedia(new File(filePath), word + " - " + translation + '\n' + caption);
        new_media.setMedia(newPhoto);

        new_message.setReplyMarkup(menuBunches());

        new_message.setCaption(word + " - " + translation + '\n' + caption);

        try {
            execute(new_media);
            try {
                execute(new_message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void iterWrongAnswersBunch(long chat_id, long wordId, long message_id, int iter, int size) {

        EditMessageCaption new_message = new EditMessageCaption();



        new_message.setChatId(chat_id);
        new_message.setMessageId(toIntExact(message_id));

        String filePath = Objects.requireNonNull(wordsRepository.findById(wordId).orElse(null)).getImagePath();
        String word = Objects.requireNonNull(wordsRepository.findById(wordId).orElse(null)).getEngword();
        String translation = Objects.requireNonNull(wordsRepository.findById(wordId).orElse(null)).getTranslation();
        String caption = Objects.requireNonNull(wordsRepository.findById(wordId).orElse(null)).getExample();

        EditMessageMedia new_media = new EditMessageMedia();
        new_media.setChatId(chat_id);
        new_media.setMessageId(toIntExact(message_id));
        InputMediaPhoto newPhoto = new InputMediaPhoto();
        newPhoto.setMedia(new File(filePath), word + " - " + translation + '\n' + caption);
        new_media.setMedia(newPhoto);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonUnderstand = new InlineKeyboardButton();

        inlineKeyboardButtonUnderstand.setText("Understand!" + rightButtonemj);
        inlineKeyboardButtonUnderstand.setCallbackData("understand");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonUnderstand);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);
        rowList.add(backToMainMenuButton());
        inlineKeyboardMarkup.setKeyboard(rowList);
        new_message.setReplyMarkup(inlineKeyboardMarkup);

        new_message.setCaption("Вам осталось повторить " + iter +"/" +size +" слов!" + '\n' +word + " - " + translation + '\n' + caption);

        Optional<User> user = userRepository.findById(chat_id);

        try {
            execute(new_media);
            try {
                execute(new_message);

                user.get().setWrongworditer(user.get().getWrongworditer()+1);
                userRepository.save(user.get());
                //outeriter++;
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void testing(long chat_id) {
        SendPhoto msgPhoto = new SendPhoto();


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonMain = new InlineKeyboardButton();
        inlineKeyboardButtonMain.setText("Let's go!");
        inlineKeyboardButtonMain.setCallbackData("test button");
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonMain);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);
        rowList.add(backToMainMenuButton());
        inlineKeyboardMarkup.setKeyboard(rowList);
        msgPhoto.setChatId(chat_id);
        msgPhoto.setReplyMarkup(inlineKeyboardMarkup);


        msgPhoto.setPhoto(new InputFile(new File(wordsRepository.findById(5001L).get().getImagePath())));
        msgPhoto.setCaption("Вы прошли "+userRepository.findById(chat_id).get().getRecentWord()+" новых слов, вам предлагается пройти тестирование.\nВам будет дано изображение и превод слова на русском - выберите один из четырех вариантов ответа.");
        try {
            execute(msgPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public Pair[] testBunch(long chat_id, long message_id) {
        Optional<User> user = userRepository.findById(chat_id);
        int range = user.get().getRecentWord();   //7
        /*int max = (int) (Math.floor(range / 10) * 10);

        if (max == 0)
            max = 10;*/
        int value = 0;
        if (range < 4 ) value = 4;
        else value = new Random().nextInt(4,range+1);



        EditMessageCaption newMessage = new EditMessageCaption();
        newMessage.setChatId(chat_id);
        newMessage.setMessageId((int) message_id);
        EditMessageMedia newMedia = new EditMessageMedia();
        newMedia.setChatId(chat_id);
        newMedia.setMessageId((int) message_id);
        InputMediaPhoto newPhoto = new InputMediaPhoto();


        String a = "";
        String b = "";
        String c = "";
        String d = "";
        String f = "";

        int[] ints = new Random().ints(1, value+1).distinct().limit(4).toArray();
        int lottery = new Random().nextInt(0, 4);

        a = wordsRepository.findById((long) ints[0]).get().getEngword();
        b = wordsRepository.findById((long) ints[1]).get().getEngword();
        c = wordsRepository.findById((long) ints[2]).get().getEngword();
        d = wordsRepository.findById((long) ints[3]).get().getEngword();
        f = wordsRepository.findById((long) ints[lottery]).get().getEngword();

        Pair pair = new Pair(Long.valueOf(ints[lottery]), f);
        Pair pair1 = new Pair(Long.valueOf(ints[0]), a);
        Pair pair2 = new Pair(Long.valueOf(ints[1]), b);
        Pair pair3 = new Pair(Long.valueOf(ints[2]), c);
        Pair pair4 = new Pair(Long.valueOf(ints[3]), d);
        Pair[] answersWithid = {pair,pair1,pair2,pair3,pair4};



        Optional<Words> wordDataFromDB = wordsRepository.findById((long) ints[lottery]);
        Words word = wordDataFromDB.get();
        String translation = word.getTranslation().replace('.', ' ');
        translation = translation.replace('(', ' ');
        translation = translation.replace(')', ' ');
        translation = translation.replace('-', '—');
        translation = translation.replace('\\', ' ');
        newMessage.setCaption("||" + translation + "||");
        newMessage.setParseMode(ParseMode.MARKDOWNV2);
        newPhoto.setMedia(new File(word.getImagePath()), "file");
        newMedia.setMedia(newPhoto);

/*
        int[] ints = new int[4];
        while (true) {
            ints = new Random().ints(1, value).distinct().limit(4).toArray();
            ints[0] = value;
            boolean flag = true;
            Arrays.sort(ints);
            int prev = 0;
            for (int j : ints) {
                // если два последовательных элемента равны,
                // найден дубликат
                if (j == prev) {
                    flag = false;
                    break;
                }
                prev = j;
            }
            if (flag)
                break;
        }
        Random random = new Random();

        for (int k = 0; k < 3; k++) {
            int index = random.nextInt(k + 1, 4);
            int temp = ints[k];
            ints[k] = ints[index];
            ints[index] = temp;
        }

        */




        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonA = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonB = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonC = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonD = new InlineKeyboardButton();




        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow4 = new ArrayList<>();


        keyboardButtonsRow1.add(inlineKeyboardButtonA);
        keyboardButtonsRow2.add(inlineKeyboardButtonB);

        keyboardButtonsRow3.add(inlineKeyboardButtonC);
        keyboardButtonsRow4.add(inlineKeyboardButtonD);


        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();


        InlineKeyboardButton inlineKeyboardButtonMain = new InlineKeyboardButton();
        inlineKeyboardButtonMain.setText(backToMenu+ "Back to menu");
        inlineKeyboardButtonMain.setCallbackData("back to menu from testing");
        List<InlineKeyboardButton> keyboardButtonsRowMainBtn = new ArrayList<>();
        keyboardButtonsRowMainBtn.add(inlineKeyboardButtonMain);



        rowList.add(keyboardButtonsRow1);
        rowList.add(keyboardButtonsRow2);
        rowList.add(keyboardButtonsRow3);
        rowList.add(keyboardButtonsRow4);
        rowList.add(keyboardButtonsRowMainBtn);


        inlineKeyboardButtonA.setText("A. " + a);
        inlineKeyboardButtonB.setText("B. " + b);
        inlineKeyboardButtonC.setText("C. " + c);
        inlineKeyboardButtonD.setText("D. " + d);
        inlineKeyboardButtonA.setCallbackData(a);
        inlineKeyboardButtonB.setCallbackData(b);
        inlineKeyboardButtonC.setCallbackData(c);
        inlineKeyboardButtonD.setCallbackData(d);

        inlineKeyboardMarkup.setKeyboard(rowList);
        newMedia.setReplyMarkup(inlineKeyboardMarkup);
        newMessage.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(newMedia);
            try {
                execute(newMessage);

            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

/*
        if (update.hasCallbackQuery() && update.getCallbackQuery() != null) {
            try {
                execute(newMedia);
                try {
                    execute(newMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
*/
        return answersWithid;
    }


    public void viewMainMenu(long chat_id, long message_id){
        EditMessageCaption new_message = new EditMessageCaption();

        Optional<User> user = userRepository.findById(chat_id);
        String name = user.get().getFirstName();
        int recentWord = user.get().getRecentWord();
        int rightAnswers = user.get().getRightAnswers();
        int answers = user.get().getAnswers();
        new_message.setCaption("Dear " + name +"! На данный момент вы уже изучили "+recentWord +"/5000 слов. \n\n" +
                "Go on! \n\n"+
                "Вы дали " + rightAnswers+"/"+answers+" правильных ответов. \n\n"+
                "Для продолжения изучения новых слов введите команду: \n /newbunch \n\n"+
                "Для прохождения тестирования по изученным словам введите команду: \n /test \n\n"+
                "P.S. \n Для сброса статистики ответов введите команду: \n /deletemydata \n\n"+
                "Все команды: \n /help \n\n"
        );
        new_message.setMessageId((int) message_id);
        new_message.setChatId(chat_id);
        try {
            execute(new_message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    Pair[] results;


    ArrayList<Long> wrongAnswers = new ArrayList<Long>();
    //int outeriter = 0;
    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    try {
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;
                case "/test":
                    wrongAnswers = new ArrayList<Long>();
                    Optional<User> usern1 = userRepository.findById(chatId);
                    usern1.get().setWrongworditer(0);
                    userRepository.save(usern1.get());
                    testing(chatId);
                    break;
                case("/deletemydata"):
                    Optional<User> user = userRepository.findById(chatId);
                    user.get().setRightAnswers(0);
                    user.get().setAnswers(0);
                    userRepository.save(user.get());
                    break;
                /*case "/mydata":
                    try {
                        execute(sendInlineKeyBoardMessage(update.getMessage().getChatId()));
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                    break;*/
                case "/faq":
                    sendFAQ(chatId);
                    break;
                case "/newbunch":
                    sendImageUploadingAFile(String.valueOf(chatId));
                    break;


                default:
                    sendMessage(chatId, "Sorry, I don't know this command");

            }

        } else if (update.hasCallbackQuery()) {
            // Set variables
            String call_data = update.getCallbackQuery().getData();
            long message_id = update.getCallbackQuery().getMessage().getMessageId();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();

            switch (call_data) {
                case ("next word"):
                    iterBunch(chat_id, message_id, true);
                    break;
                case ("previous word"):
                    iterBunch(chat_id, message_id, false);
                    break;
                case ("test button"):
                    results = testBunch(chat_id, message_id);
                    break;

                case("understand"):

                    Optional<User> user = userRepository.findById(chat_id);
                    if((user.get().getWrongworditer()) >= wrongAnswers.size()){
                    //if(outeriter >= wrongAnswers.size()){
                        viewMainMenu(chat_id, message_id);
                        break;
                    }

                    iterWrongAnswersBunch(chat_id,wrongAnswers.get((user.get().getWrongworditer())),message_id,wrongAnswers.size()-(user.get().getWrongworditer()),wrongAnswers.size());
                    //iterWrongAnswersBunch(chat_id,wrongAnswers.get(outeriter),message_id,wrongAnswers.size()-outeriter,wrongAnswers.size());


                    /*
                    for (int abc = 1; abc < wrongAnswers.size(); abc++) {
                        iterWrongAnswersBunch(chat_id,wrongAnswers.get(abc),message_id,wrongAnswers.size()-abc,wrongAnswers.size());
                    }

                    */
                    break;


                case("back to menu from testing"):
                    if (wrongAnswers.size()!=0){
                        Optional<User> usern = userRepository.findById(chat_id);
                        usern.get().setWrongworditer(0);
                        userRepository.save(usern.get());
                        //outeriter=0;
                        iterWrongAnswersBunch(chat_id,wrongAnswers.get(0),message_id,wrongAnswers.size(),wrongAnswers.size());

                        /*for (int abc = 0; abc < wrongAnswers.size(); abc++) {
                            iterWrongAnswersBunch(chat_id,wrongAnswers.get(abc),message_id,wrongAnswers.size()-abc,wrongAnswers.size());
                        }*/


                    }else{
                        Optional<User> usern1 = userRepository.findById(chat_id);
                        usern1.get().setWrongworditer(0);
                        userRepository.save(usern1.get());
                        viewMainMenu(chat_id, message_id);
                    }

                    break;
                case("back to menu"):
                    Optional<User> usern1 = userRepository.findById(chat_id);
                    usern1.get().setWrongworditer(0);
                    userRepository.save(usern1.get());
                    viewMainMenu(chat_id, message_id);
                    break;
                default: break;
            }
            if (call_data.equals(results[1].getValue()) ||
                call_data.equals(results[2].getValue()) ||
                call_data.equals(results[3].getValue()) ||
                call_data.equals(results[4].getValue())
            ) {
                Optional<User> user = userRepository.findById(chat_id);
                user.get().setAnswers(user.get().getAnswers() + 1);
                if(call_data.equals(results[0].getValue())){
                    user.get().setRightAnswers(user.get().getRightAnswers() + 1);
                    userRepository.save(user.get());

                } else{
                    userRepository.save(user.get());
                    wrongAnswers.add((Long) results[0].getKey());
                }

                results = testBunch(chat_id, message_id);

            }
        }
    }



    private void sendFAQ(long chatId) {
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);
        message.setChatId(chatId);
        message.setText(FAQ);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void startCommandReceived(long chatId, String name) throws TelegramApiException {
        String answer = "Hi, " + name + "!";
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
        execute(new SendSticker(String.valueOf(chatId), new InputFile("CAACAgIAAxkBAAEJebBkmGBGUe23rYGVfIlp4mahAXsxjAACBQAD9xeBK95mq4g2GUDbLwQ")));
        sendFAQ(chatId);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message);

        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());

        }
    }
}


