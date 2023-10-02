package com.example.vocabularybot.service;

import com.example.vocabularybot.config.BotConfig;
import com.example.vocabularybot.model.*;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.hibernate.cfg.Configuration;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAudio;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.*;

import static java.lang.Math.toIntExact;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {


    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WordsRepository wordsRepository;
    @Autowired
    private FavoriteWordsRepository favoriteWordsRepository;
    final BotConfig config;

    final String audioPath = "\\media\\audio";

    private final String rightButtonemj = EmojiParser.parseToUnicode(":arrow_right:");
    private final String leftButtonemj = EmojiParser.parseToUnicode(":arrow_left:");
    private final String backToMenu = EmojiParser.parseToUnicode(":back:");
    private final String favorite = EmojiParser.parseToUnicode(":star:");
    private final String removeFavorite = EmojiParser.parseToUnicode(":no_entry_sign:");
    private final String sadFace = EmojiParser.parseToUnicode(":pensive:");
    private final String stonks = EmojiParser.parseToUnicode("\uD83D\uDCC8");
    private final String bunchSmile = EmojiParser.parseToUnicode("\uD83D\uDCD6");
    private final String learn = EmojiParser.parseToUnicode("\uD83E\uDDD1\u200D\uD83C\uDFEB");

    static final String HELP_TEXT = "Информация о командах: \n\n" +
            "/start " + "регистрация и получение стартового сообщения\n\n" +
            "/newbunch " + "перейти к изучению слов\n\n" +
            "/favorite" + "Перейти к избранный словам\n\n" +
            "/test " + "пройти тестирование по изученным словам\n\n" +
            "/faq " + "ответы на часто задаваемые вопросы\n\n" +
            "/help " + "информация о командах\n\n" +
            "/deletemydata " + "обнулить мои ответы\n\n" +
            "Вопросы по боту: @hehheheehe";


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
        listOfCommands.add(new BotCommand("/newbunch", "Перейти к изучению слов."));
        listOfCommands.add(new BotCommand("/favorite", "Перейти к избранный словам."));
        listOfCommands.add(new BotCommand("/test", "Пройти тестирование по изученным словам."));
        listOfCommands.add(new BotCommand("/faq", "Ответы на часто задаваемые вопросы."));
        listOfCommands.add(new BotCommand("/help", "Информация о командах."));
        listOfCommands.add(new BotCommand("/deletemydata", "Обнулить мои ответы."));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));

        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }

    }

   /* public List<FavoriteWords> getNeighboringWords(Long id) {

        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
        Session session = sessionFactory.openSession();



        String hql = "FROM FavoriteWord fw " +
                "WHERE fw.id = (SELECT MAX(id) FROM FavoriteWord WHERE id < :id) " +
                "   OR fw.id = (SELECT MIN(id) FROM FavoriteWord WHERE id > :id)";

        return sessionFactory.getCurrentSession()
                .createQuery(hql, FavoriteWords.class)
                .setParameter("id", id)
                .list();
    }*/


    public EditMessageReplyMarkup favoriteMenu(Long favorWordId, long chatId, long linkKeyboardMsgID) {


        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
        Session session = sessionFactory.openSession();
//        Query query = session.createQuery("from FavoriteWords where word_id = :wordId and chat_id = :chatId");
//
//        query.setParameter("wordId", favorWordId);
//        query.setParameter("chatId", chatId);
//        List list = query.list(); //FavoriteWords{favorite_words_id=24, chat_id=489318704, word_id=1012}


//        FavoriteWords recentFavorWord = (FavoriteWords) list.get(0);
        Optional<FavoriteWords> favoriteObject = favoriteWordsRepository.findById(favorWordId);
        FavoriteWords recentFavoriteObject = favoriteObject.get();


//        TODO думай дальше, queryPrev.setMaxResults(1).uniqueResult() возвращает список элементов,
//         каждый из которых <  favorite_words_id. setMaxResults(1) берет только 1 значение, и оно 1ое из списка


//        Query queryPrev = session.createQuery("from FavoriteWords where favorite_words_id<:tableId and chat_id = :chatId");
//        Query queryNext = session.createQuery("from FavoriteWords where favorite_words_id>:tableId and chat_id = :chatId");
//
        Query queryPrev = session.createQuery("from FavoriteWords where favorite_words_id = (SELECT MAX(favorite_words_id) FROM FavoriteWords WHERE favorite_words_id < :tableId) and chat_id = :chatId");
        Query queryNext = session.createQuery("from FavoriteWords where favorite_words_id = (SELECT MIN(favorite_words_id) FROM FavoriteWords WHERE favorite_words_id > :tableId) and chat_id = :chatId");


        queryPrev.setParameter("tableId", favorWordId);
        queryPrev.setParameter("chatId", chatId);
        queryNext.setParameter("tableId", favorWordId);
        queryNext.setParameter("chatId", chatId);


        FavoriteWords prevWord = (FavoriteWords) queryPrev.setMaxResults(1).uniqueResult();
        FavoriteWords nextWord = (FavoriteWords) queryNext.setMaxResults(1).uniqueResult();

        Query queryMax = session.createQuery("select max(favorite_words_id) from FavoriteWords where chat_id = :chatId");
        Query queryMin = session.createQuery("select min(favorite_words_id) from FavoriteWords where chat_id = :chatId");

//        queryMax.setParameter("tableId", favorWordId);
        queryMax.setParameter("chatId", chatId);
//        queryMin.setParameter("tableId", favorWordId);
        queryMin.setParameter("chatId", chatId);


        Optional<FavoriteWords> maxWordObj = favoriteWordsRepository.findById((Long) queryMax.setMaxResults(1).uniqueResult());
        FavoriteWords maxWord = maxWordObj.get();

        Optional<FavoriteWords> minWordObj = favoriteWordsRepository.findById((Long) queryMin.setMaxResults(1).uniqueResult());
        FavoriteWords minWord = minWordObj.get();

//        FavoriteWords minWord = (FavoriteWords) queryMin.setMaxResults(1).uniqueResult();
        if (prevWord == null && nextWord == null) {
            sendMessage(chatId, "У вас пока только одно избранное слово");
            prevWord = recentFavoriteObject;
            nextWord = recentFavoriteObject;
        }

        if (prevWord == null) {
            prevWord = maxWord;
        }
        if (nextWord == null) {
            nextWord = minWord;
        }


        InlineKeyboardMarkup keyboardMarkup = menuBunches();
        EditMessageReplyMarkup newKeyboard = new EditMessageReplyMarkup();
        newKeyboard.setChatId(chatId);
        newKeyboard.setMessageId((int) linkKeyboardMsgID);

        keyboardMarkup.getKeyboard().get(1).get(0).setCallbackData("remove favorite" + ":" + recentFavoriteObject.getWord_id());
        keyboardMarkup.getKeyboard().get(0).get(0).setCallbackData("prevFavor" + ":" + prevWord.getFavorite_words_id());
        keyboardMarkup.getKeyboard().get(0).get(1).setCallbackData("nextFavor" + ":" + nextWord.getFavorite_words_id());
        newKeyboard.setReplyMarkup(keyboardMarkup);
        return newKeyboard;
    }


    public BunchReceiver favoriteInitMenu(long chatId) {
        BunchReceiver firstBunch = null;

        Optional<User> user = userRepository.findById(chatId);
        Long favorite_word = (long) user.get().getFavoriteworditer();
        if (favorite_word == 0 || favoriteWordsRepository.findById(favorite_word).get() == null) {
            sendMessage(chatId, "У вас пока еще нет избранных слов" + sadFace);
            return null;
        }
        Optional<FavoriteWords> favoriteWordObject = favoriteWordsRepository.findById(favorite_word);
        FavoriteWords favoriteWordFromTable = favoriteWordObject.get();


        Optional<Words> wordDataFromDB = wordsRepository.findById(favoriteWordFromTable.getWord_id());
        Words word = wordDataFromDB.get();

        // Create send method
        SendPhoto sendPhotoRequest = new SendPhoto();
        sendPhotoRequest.setChatId(chatId);
        sendPhotoRequest.setPhoto(new InputFile(new File(word.getImagePath())));
        sendPhotoRequest.setCaption(word.getEngword() + " - " + word.getTranslation() + '\n' + word.getExample());


//        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
//        Session session = sessionFactory.openSession();
//        Query query = session.createQuery("from FavoriteWords where word_id = :wordId and chat_id = :chatId");
//
//        query.setParameter("wordId", favorite_word);
//        query.setParameter("chatId", chatId);
//        List list = query.list(); //FavoriteWords{favorite_words_id=24, chat_id=489318704, word_id=1012}
//
//        FavoriteWords recentFavorWord = (FavoriteWords) list.get(0);
//        Long tableFavorId = recentFavorWord.getFavorite_words_id();
//
//        Query queryPrev = session.createQuery("from FavoriteWords where favorite_words_id<:tableId and chat_id = :chatId");
//        Query queryNext = session.createQuery("from FavoriteWords where favorite_words_id>:tableId and chat_id = :chatId");
//
//        queryPrev.setParameter("tableId", tableFavorId);
//        queryPrev.setParameter("chatId", chatId);
//        queryNext.setParameter("tableId", tableFavorId);
//        queryNext.setParameter("chatId", chatId);
//
//
//        FavoriteWords prevWord = (FavoriteWords) queryPrev.setMaxResults(1).uniqueResult();
//        FavoriteWords nextWord = (FavoriteWords) queryNext.setMaxResults(1).uniqueResult();
//        if (prevWord == null) prevWord = nextWord;
//        if (nextWord == null) nextWord = prevWord;
//        if (prevWord == null && nextWord == null) {
//            prevWord = recentFavorWord;
//            nextWord = recentFavorWord;
//        }
//
//
//        InlineKeyboardMarkup keyboardMarkup = menuBunches();
//        EditMessageReplyMarkup newKeyboard = new EditMessageReplyMarkup();
//        newKeyboard.setChatId(chatId);
//        try {
//            newKeyboard.setMessageId(execute(sendPhotoRequest).getMessageId());
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//        keyboardMarkup.getKeyboard().get(1).get(0).setCallbackData("remove favorite" + ":" + favorite_word);
//        keyboardMarkup.getKeyboard().get(0).get(0).setCallbackData("prevFavor" + ":" + prevWord.getFavorite_words_id());
//        keyboardMarkup.getKeyboard().get(0).get(1).setCallbackData("nextFavor" + ":" + nextWord.getFavorite_words_id());
//        newKeyboard.setReplyMarkup(keyboardMarkup);
        try {
            // Execute the method

            execute(favoriteMenu(favorite_word, chatId, execute(sendPhotoRequest).getMessageId()));


        } catch (TelegramApiException e) {
            log.error("Error with favoriteinimenu()",favorite_word,chatId);
            e.printStackTrace();
        }


        SendAudio wordAudio = new SendAudio();
        wordAudio.setChatId(chatId);
        wordAudio.setAudio(
                new InputFile(
                        new File(audioPath + "\\word\\" + favorite_word + "w.mp3"),
                        word.getEngword().replace("\\", ""))
        );

        SendAudio contextAudio = new SendAudio();
        contextAudio.setChatId(chatId);
        contextAudio.setAudio(new InputFile(
                new File(
                        audioPath + "\\context\\" + favorite_word + "c.mp3"),
                word.getExample().replace("\\", ""))
        );
        try {

            firstBunch = new BunchReceiver(String.valueOf(chatId),
                    execute(wordAudio).getMessageId(),
                    execute(contextAudio).getMessageId()
            );

        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        return firstBunch;
    }

    public void iterFavorite(long chatId, long messageId, BunchReceiver lastAudios) {
        EditMessageCaption newMessage = new EditMessageCaption();
        newMessage.setChatId(chatId);
        newMessage.setMessageId((int) messageId);

        Optional<User> user = userRepository.findById(Long.valueOf(chatId));
        Long favoriteWordIterId = Long.valueOf(user.get().getFavoriteworditer());
        Optional<FavoriteWords> fuckmeilovehibernate = favoriteWordsRepository.findById(favoriteWordIterId);
        FavoriteWords favorWordObj = fuckmeilovehibernate.get();
        Optional<Words> wordDataFromDB = wordsRepository.findById(favorWordObj.getWord_id());
        Words word = wordDataFromDB.get();


        EditMessageMedia newMedia = new EditMessageMedia();
        newMedia.setChatId(chatId);
        newMedia.setMessageId(toIntExact(messageId));
        InputMediaPhoto newPhoto = new InputMediaPhoto();
        newPhoto.setMedia(new File(word.getImagePath()), word.getEngword() + " - " + word.getTranslation() + '\n' + word.getExample());
        newMedia.setMedia(newPhoto);

        newMessage.setCaption(word.getEngword() + " - " + word.getTranslation() + '\n' + word.getExample());


        EditMessageMedia wordAudio = new EditMessageMedia();
        wordAudio.setChatId(chatId);
        wordAudio.setMessageId(lastAudios.getMessageAudioWordID());
        InputMediaAudio newWordAudio = new InputMediaAudio();
        newWordAudio.setMedia(new File(audioPath + "\\word\\" + favorWordObj.getWord_id() + "w.mp3"), word.getEngword().replace("\\", ""));
        wordAudio.setMedia(newWordAudio);


        EditMessageMedia contextAudio = new EditMessageMedia();
        contextAudio.setChatId(chatId);
        contextAudio.setMessageId(lastAudios.getMessageAudioContextID());
        InputMediaAudio newContextAudio = new InputMediaAudio();
        newContextAudio.setMedia(new File(audioPath + "\\context\\" + favorWordObj.getWord_id() + "c.mp3"), word.getExample().replace("\\", ""));
        contextAudio.setMedia(newContextAudio);

        try {

            execute(newMedia);
            try {
                execute(newMessage);
                execute(favoriteMenu(favoriteWordIterId, chatId, messageId));
                execute(wordAudio);
                execute(contextAudio);

            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } catch (TelegramApiException e) {
            log.error("iterFavorite()", chatId);
            e.printStackTrace();
        }


    }

    public BunchReceiver sendImageUploadingAFile(String chatId) {
        Optional<User> user = userRepository.findById(Long.valueOf(chatId));
        Long recent_word = Long.valueOf(user.get().getRecentWord());

        Optional<Words> wordDataFromDB = wordsRepository.findById(recent_word);
        Words word = wordDataFromDB.get();


        // Create send method
        SendPhoto sendPhotoRequest = new SendPhoto();
/*
        SendMediaGroup sendPhotoCaptionAndAudio = new SendMediaGroup();
        sendPhotoCaptionAndAudio.setChatId(chatId);
        List<InputMedia<?>> mediaList = new InputFile[]{new InputFile(new File(word.getImagePath())),};
        InputMedia<?>[] mediaArray = mediaList.toArray(InputMedia[]::new);
        sendPhotoCaptionAndAudio.setMedias();
*/
        sendPhotoRequest.setCaption(word.getEngword() + " - " + word.getTranslation() + '\n' + word.getExample());

        InlineKeyboardMarkup keyboardMarkup = menuBunches();
        EditMessageReplyMarkup newKeyboard = new EditMessageReplyMarkup();
        newKeyboard.setChatId(chatId);


        // Set destination chat id
        sendPhotoRequest.setChatId(chatId);
        // Set the photo file as a new photo (You can also use InputStream with a constructor overload)
//        sendPhotoRequest.setPhoto(new InputFile(new File(filePath)));
        sendPhotoRequest.setPhoto(new InputFile(new File(word.getImagePath())));

        SendAudio wordAudio = new SendAudio();
        wordAudio.setChatId(chatId);
        wordAudio.setAudio(new InputFile(new File(audioPath + "\\word\\" + recent_word + "w.mp3"), word.getEngword().replace("\\", "")));
        SendAudio contextAudio = new SendAudio();
        contextAudio.setChatId(chatId);
        contextAudio.setAudio(new InputFile(new File(audioPath + "\\context\\" + recent_word + "c.mp3"), word.getExample().replace("\\", "")));

        BunchReceiver firstBunch = null;
        try {
            // Execute the method

            newKeyboard.setMessageId(execute(sendPhotoRequest).getMessageId());
            keyboardMarkup.getKeyboard().get(1).get(0).setCallbackData("remove favorite" + ":" + recent_word);
            newKeyboard.setReplyMarkup(keyboardMarkup);
            execute(newKeyboard);

//            execute(wordAudio);
//            execute(contextAudio);

            firstBunch = new BunchReceiver(chatId, execute(wordAudio).getMessageId(), execute(contextAudio).getMessageId());

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return firstBunch;
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
        InlineKeyboardButton inlineKeyboardButtonFavorite = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButtonRemoveFromFavorite = new InlineKeyboardButton();


        inlineKeyboardButtonRight.setText("Next" + rightButtonemj);
        inlineKeyboardButtonRight.setCallbackData("next word");
        inlineKeyboardButtonLeft.setText(leftButtonemj + "Previous");
        inlineKeyboardButtonLeft.setCallbackData("previous word");
        inlineKeyboardButtonFavorite.setText(favorite + "Add to favorite" + favorite);
        inlineKeyboardButtonFavorite.setCallbackData("favorite");
        inlineKeyboardButtonRemoveFromFavorite.setText(removeFavorite + "Remove from favorite" + removeFavorite);
        inlineKeyboardButtonRemoveFromFavorite.setCallbackData("remove favorite");
        List<InlineKeyboardButton> keyboardButtonsRowArrows = new ArrayList<>();
        keyboardButtonsRowArrows.add(inlineKeyboardButtonLeft);
        keyboardButtonsRowArrows.add(inlineKeyboardButtonRight);
        List<InlineKeyboardButton> keyboardButtonsRowFavorite = new ArrayList<>();
        keyboardButtonsRowFavorite.add(inlineKeyboardButtonRemoveFromFavorite);
        keyboardButtonsRowFavorite.add(inlineKeyboardButtonFavorite);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRowArrows);
        rowList.add(keyboardButtonsRowFavorite);
        rowList.add(backToMainMenuButton());
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }

    public List<InlineKeyboardButton> backToMainMenuButton() {
        InlineKeyboardButton inlineKeyboardButtonMain = new InlineKeyboardButton();
        inlineKeyboardButtonMain.setText(backToMenu + "Back to menu");
        inlineKeyboardButtonMain.setCallbackData("back to menu");
        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow2.add(inlineKeyboardButtonMain);
        return keyboardButtonsRow2;
    }

    public void iterBunch(long chatId, long messageId, BunchReceiver firstLayot, boolean direction) {

        EditMessageCaption new_message = new EditMessageCaption();

        Optional<User> user = userRepository.findById(chatId);
        if (direction) user.get().setRecentWord(user.get().getRecentWord() + 1);
        else if (!direction && user.get().getRecentWord() > 1) user.get().setRecentWord(user.get().getRecentWord() - 1);

        userRepository.save(user.get());
        new_message.setChatId(chatId);
        new_message.setMessageId(toIntExact(messageId));

        Long recentWordId = Long.valueOf(Objects.requireNonNull(userRepository.findById(chatId).orElse(null)).getRecentWord());
        String filePath = Objects.requireNonNull(wordsRepository.findById(recentWordId).orElse(null)).getImagePath();
        String word = Objects.requireNonNull(wordsRepository.findById(recentWordId).orElse(null)).getEngword();
        String translation = Objects.requireNonNull(wordsRepository.findById(recentWordId).orElse(null)).getTranslation();
        String caption = Objects.requireNonNull(wordsRepository.findById(recentWordId).orElse(null)).getExample();

        EditMessageMedia new_media = new EditMessageMedia();
        new_media.setChatId(chatId);
        new_media.setMessageId(toIntExact(messageId));
        InputMediaPhoto newPhoto = new InputMediaPhoto();
        newPhoto.setMedia(new File(filePath), word + " - " + translation + '\n' + caption);
        new_media.setMedia(newPhoto);


        InlineKeyboardMarkup keyboardMarkup = menuBunches();
        EditMessageReplyMarkup newKeyboard = new EditMessageReplyMarkup();
        newKeyboard.setChatId(chatId);
        newKeyboard.setMessageId((int) messageId);
        keyboardMarkup.getKeyboard().get(1).get(0).setCallbackData("remove favorite" + ":" + recentWordId);
        newKeyboard.setReplyMarkup(keyboardMarkup);


        new_message.setCaption(word + " - " + translation + '\n' + caption);


        EditMessageMedia wordAudio = new EditMessageMedia();
        wordAudio.setChatId(chatId);
        wordAudio.setMessageId(firstLayot.getMessageAudioWordID());
        InputMediaAudio newWordAudio = new InputMediaAudio();
        newWordAudio.setMedia(new File(audioPath + "\\word\\" + recentWordId + "w.mp3"), word.replace("\\", ""));
        wordAudio.setMedia(newWordAudio);


        EditMessageMedia contextAudio = new EditMessageMedia();
        contextAudio.setChatId(chatId);
        contextAudio.setMessageId(firstLayot.getMessageAudioContextID());
        InputMediaAudio newContextAudio = new InputMediaAudio();
        newContextAudio.setMedia(new File(audioPath + "\\context\\" + recentWordId + "c.mp3"), caption.replace("\\", ""));
        contextAudio.setMedia(newContextAudio);


        try {
            execute(new_media);
            try {
                execute(new_message);
                execute(newKeyboard);
                execute(wordAudio);
                execute(contextAudio);

            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void iterWrongAnswersBunch(long chatId, long wordId, long messageId, int iter, int size) {

        EditMessageCaption new_message = new EditMessageCaption();


        new_message.setChatId(chatId);
        new_message.setMessageId(toIntExact(messageId));

        String filePath = Objects.requireNonNull(wordsRepository.findById(wordId).orElse(null)).getImagePath();
        String word = Objects.requireNonNull(wordsRepository.findById(wordId).orElse(null)).getEngword();
        String translation = Objects.requireNonNull(wordsRepository.findById(wordId).orElse(null)).getTranslation();
        String caption = Objects.requireNonNull(wordsRepository.findById(wordId).orElse(null)).getExample();

        EditMessageMedia new_media = new EditMessageMedia();
        new_media.setChatId(chatId);
        new_media.setMessageId(toIntExact(messageId));
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

        new_message.setCaption("Вам осталось повторить " + iter + "/" + size + " слов!" + '\n' + word + " - " + translation + '\n' + caption);

        Optional<User> user = userRepository.findById(chatId);

        try {
            execute(new_media);
            try {
                execute(new_message);

                user.get().setWrongworditer(user.get().getWrongworditer() + 1);
                userRepository.save(user.get());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void testing(long chatId) {
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
        msgPhoto.setChatId(chatId);
        msgPhoto.setReplyMarkup(inlineKeyboardMarkup);


        msgPhoto.setPhoto(new InputFile(new File(wordsRepository.findById(5001L).get().getImagePath())));
        msgPhoto.setCaption("Вы прошли " + userRepository.findById(chatId).get().getRecentWord() + " новых слов, вам предлагается пройти тестирование.\nВам будет дано изображение и превод слова на русском - выберите один из четырех вариантов ответа.");
        try {
            execute(msgPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void testBunch(long chatId, long messageId) {
        Optional<User> user = userRepository.findById(chatId);
        int range = user.get().getRecentWord();   //7
        /*int max = (int) (Math.floor(range / 10) * 10);

        if (max == 0)
            max = 10;*/
        int value = 0;
        if (range < 4) value = 4;
        else value = new Random().nextInt(4, range + 1);


        EditMessageCaption newMessage = new EditMessageCaption();
        newMessage.setChatId(chatId);
        newMessage.setMessageId((int) messageId);
        EditMessageMedia newMedia = new EditMessageMedia();
        newMedia.setChatId(chatId);
        newMedia.setMessageId((int) messageId);
        InputMediaPhoto newPhoto = new InputMediaPhoto();


        String a ,b, c, d, f;


        int[] ints = new Random().ints(1, value + 1).distinct().limit(4).toArray();
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
        Pair[] answersWithid = {pair, pair1, pair2, pair3, pair4};
        resultsMap.put(String.valueOf(chatId), answersWithid);


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
        inlineKeyboardButtonMain.setText(backToMenu + "Back to menu");
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

    }


    public void viewMainMenu(long chatId, long messageId) {
        EditMessageCaption new_message = new EditMessageCaption();

        Optional<User> user = userRepository.findById(chatId);
        String name = user.get().getFirstName();
        int recentWord = user.get().getRecentWord();
        int rightAnswers = user.get().getRightAnswers();
        int answers = user.get().getAnswers();
        new_message.setCaption("Dear " + name + "! На данный момент вы уже изучили " + recentWord + "/5000 слов. \n\n" +
                "Go on! \n\n" +
                "Вы дали " + rightAnswers + "/" + answers + " правильных ответов."+stonks+"\n\n" +
                "Для продолжения изучения новых слов введите команду: \n /newbunch \n\n" +
                "Для прохождения тестирования по изученным словам введите команду: \n /test \n\n" +
                "P.S. \n Для сброса статистики ответов введите команду: \n /deletemydata \n\n" +
                "Все команды: \n /help \n\n"
        );
        new_message.setMessageId((int) messageId);
        new_message.setChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonFavorWords = new InlineKeyboardButton();
        inlineKeyboardButtonFavorWords.setText(favorite + "favorite words" + favorite);
        inlineKeyboardButtonFavorWords.setCallbackData("view favorite from menu");
        InlineKeyboardButton inlineKeyboardButtonNewBunch = new InlineKeyboardButton();
        inlineKeyboardButtonNewBunch.setText(bunchSmile + "Учить слова" + bunchSmile);
        inlineKeyboardButtonNewBunch.setCallbackData("new bunch from menu");
        InlineKeyboardButton inlineKeyboardTest = new InlineKeyboardButton();
        inlineKeyboardTest.setText(learn + "Test" + learn);
        inlineKeyboardTest.setCallbackData("test from menu");
        List<InlineKeyboardButton> keyboardButtonsRowFavorite = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRowNewBunch = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRowTest = new ArrayList<>();
        keyboardButtonsRowFavorite.add(inlineKeyboardButtonFavorWords);
        keyboardButtonsRowNewBunch.add(inlineKeyboardButtonNewBunch);
        keyboardButtonsRowTest.add(inlineKeyboardTest);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRowFavorite);
        rowList.add(keyboardButtonsRowNewBunch);
        rowList.add(keyboardButtonsRowTest);
        inlineKeyboardMarkup.setKeyboard(rowList);
        new_message.setReplyMarkup(inlineKeyboardMarkup);


        try {
            execute(new_message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }


    HashMap<String, ArrayList<Long>> wrongAnswersMap = new HashMap<>();
    HashMap<String, Pair[]> resultsMap = new HashMap<>();
    HashMap<String, BunchReceiver> lastAudios = new HashMap<>();

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
                    wrongAnswersMap.put(String.valueOf(chatId), new ArrayList<Long>());
                    Optional<User> usern1 = userRepository.findById(chatId);
                    usern1.get().setWrongworditer(0);
                    userRepository.save(usern1.get());
                    testing(chatId);
                    break;
                case ("/deletemydata"):
                    Optional<User> user = userRepository.findById(chatId);
                    user.get().setRightAnswers(0);
                    user.get().setAnswers(0);
                    userRepository.save(user.get());
                    break;

                case "/faq":
                    sendFAQ(chatId);
                    break;
                case "/newbunch":
                    BunchReceiver firstLayout = sendImageUploadingAFile(String.valueOf(chatId));
                    lastAudios.put(String.valueOf(chatId), firstLayout);
                    break;

                case "/favorite":
                    Optional<User> userCheckNullFavor = userRepository.findById(chatId);
                    BunchReceiver firstFavor = favoriteInitMenu(chatId);
                    lastAudios.put(String.valueOf(chatId), firstFavor);

                    break;
                default:
                    sendMessage(chatId, "Sorry, I don't know this command");

            }

        } else if (update.hasCallbackQuery()) {
            // Set variables
            String callData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            Pair[] results = resultsMap.get(String.valueOf(chatId));
            String[] callBackData = callData.split(":");
            switch (callBackData[0]) {
                case ("newbunch"):
                    BunchReceiver firstLayout = sendImageUploadingAFile(String.valueOf(chatId));
                    lastAudios.put(String.valueOf(chatId), firstLayout);

                    break;
                case ("next word"):
                    iterBunch(chatId, messageId, lastAudios.get(String.valueOf(chatId)), true);
                    break;
                case ("previous word"):
                    iterBunch(chatId, messageId, lastAudios.get(String.valueOf(chatId)), false);
                    break;
                case ("favorite"):
                    addToFavorite(chatId);
                    break;
                case ("remove favorite"):

                    removeFromFavorite(chatId, callBackData[1]);
                    break;
                case ("test button"):
                    testBunch(chatId, messageId);
                    break;

                case ("view favorite from menu"):
                    Optional<User> userCheckNullFavor = userRepository.findById(chatId);
                    BunchReceiver firstFavor = favoriteInitMenu(chatId);
                    lastAudios.put(String.valueOf(chatId), firstFavor);

                    break;

                case("test from menu"):
                    wrongAnswersMap.put(String.valueOf(chatId), new ArrayList<Long>());
                    Optional<User> userForTest = userRepository.findById(chatId);
                    userForTest.get().setWrongworditer(0);
                    userRepository.save(userForTest.get());
                    testing(chatId);
                    break;

                case("new bunch from menu"):
                    BunchReceiver firstLayoutFromMenu = sendImageUploadingAFile(String.valueOf(chatId));
                    lastAudios.put(String.valueOf(chatId), firstLayoutFromMenu);


                    break;
                case ("prevFavor"):
                    //TODO calling some method like iterFavorite() -> look for iterBunch()
                    // TODO add changing interator of recentFavoriteWord counter
//                    favoriteInitMenu(chat_id, message_id, lastAudios.get(String.valueOf(chat_id)), false);
                    Optional<FavoriteWords> wordPrev = favoriteWordsRepository.findById(Long.valueOf(callBackData[1]));
                    Optional<User> reduceFavorWordId = userRepository.findById(chatId);
                    User reduceFavorWordIdUser = reduceFavorWordId.get();
                    reduceFavorWordIdUser.setFavoriteworditer(Integer.parseInt(callBackData[1]));
                    userRepository.save(reduceFavorWordIdUser);

                    iterFavorite(chatId, messageId, lastAudios.get(String.valueOf(chatId)));
                    break;


                case ("nextFavor"):
                    Optional<FavoriteWords> wordNext = favoriteWordsRepository.findById(Long.valueOf(callBackData[1]));
                    //TODO calling some method like iterFavorite() -> look for iterBunch()
                    // TODO add changing interator of recentFavoriteWord counter
//                    favoriteInitMenu(chat_id, message_id, lastAudios.get(String.valueOf(chat_id)), false);
                    Optional<User> increaseFavorWordId = userRepository.findById(chatId);
                    User increaseFavorWordIdUser = increaseFavorWordId.get();
                    increaseFavorWordIdUser.setFavoriteworditer(Integer.parseInt(callBackData[1]));
                    userRepository.save(increaseFavorWordIdUser);

                    iterFavorite(chatId, messageId, lastAudios.get(String.valueOf(chatId)));
                    break;

                case ("understand"):

                    Optional<User> user = userRepository.findById(chatId);
                    if ((user.get().getWrongworditer()) >= wrongAnswersMap.get(String.valueOf(chatId)).size()) {
                        viewMainMenu(chatId, messageId);
                        break;
                    }
                    iterWrongAnswersBunch(
                            chatId,
                            wrongAnswersMap.get(
                                    String.valueOf(chatId)).get((user.get().getWrongworditer())),
                            messageId,
                            wrongAnswersMap.get(
                                    String.valueOf(chatId)).size() - (user.get().getWrongworditer()
                            ),
                            wrongAnswersMap.get(String.valueOf(chatId)).size()
                    );
                    break;


                case ("back to menu from testing"):
                    backToMenuFromTesting(chatId, messageId);
                    break;
                case ("back to menu"):
                    Optional<User> usern1 = userRepository.findById(chatId);
                    usern1.get().setWrongworditer(0);
                    userRepository.save(usern1.get());
                    viewMainMenu(chatId, messageId);
                    break;
                default:
                    break;
            }
            if (results != null) {
                if (
                        callData.equals(results[1].getValue()) ||
                                callData.equals(results[2].getValue()) ||
                                callData.equals(results[3].getValue()) ||
                                callData.equals(results[4].getValue())
                ) {
                    Optional<User> user = userRepository.findById(chatId);
                    user.get().setAnswers(user.get().getAnswers() + 1);
                    if (callData.equals(results[0].getValue())) {
                        user.get().setRightAnswers(user.get().getRightAnswers() + 1);
                        userRepository.save(user.get());

                    } else {
                        userRepository.save(user.get());
                        wrongAnswersMap.get(String.valueOf(chatId)).add((Long) results[0].getKey());
//                        wrongAnswers.add((Long) results[0].getKey());
//                        wrongAnswersMap.put(String.valueOf(chat_id),wrongAnswersMap.get(String.valueOf(chat_id)).add((Long) results[0].getKey()));
                    }

                    testBunch(chatId, messageId);

                }
            }
        }
    }

    private void backToMenuFromTesting(long chatId, long messageId) {
        if (wrongAnswersMap.get(String.valueOf(chatId)).size() != 0) {
            Optional<User> usern = userRepository.findById(chatId);
            usern.get().setWrongworditer(0);
            userRepository.save(usern.get());

            iterWrongAnswersBunch(chatId, wrongAnswersMap.get(String.valueOf(chatId)).get(0), messageId, wrongAnswersMap.get(String.valueOf(chatId)).size(), wrongAnswersMap.get(String.valueOf(chatId)).size());

        } else {
            Optional<User> usern1 = userRepository.findById(chatId);
            usern1.get().setWrongworditer(0);
            userRepository.save(usern1.get());
            viewMainMenu(chatId, messageId);
        }
    }

    private void removeFromFavorite(long chatId, String wordId) {
        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
        Session session = sessionFactory.openSession();
        Query query = session.createQuery("from FavoriteWords where word_id = :wordId and chat_id = :chatId");

        query.setParameter("wordId", wordId);
        query.setParameter("chatId", chatId);
        List list = query.list();

        FavoriteWords objectToDelete = (FavoriteWords) list.get(0);
        favoriteWordsRepository.delete(objectToDelete);


        Query nullDetector = session.createQuery("from FavoriteWords where chat_id = :chatId");
        nullDetector.setParameter("chatId", chatId);
        FavoriteWords recentFavoriteObject = (FavoriteWords) nullDetector.setMaxResults(1).uniqueResult();
        User userSetNewFavoriteIter = userRepository.findById(chatId).get();
        userSetNewFavoriteIter.setFavoriteworditer(toIntExact(recentFavoriteObject.getFavorite_words_id()));
        userRepository.save(userSetNewFavoriteIter);


    }

    private void addToFavorite(long chatId) {
        Optional<User> userForFavor = userRepository.findById(chatId);

        Long favoriteTableInsertWordID = Long.valueOf(userForFavor.get().getRecentWord());
        FavoriteWords favoriteWord = new FavoriteWords();
        favoriteWord.setChat_id(chatId);
        favoriteWord.setWord_id(favoriteTableInsertWordID);
        favoriteWordsRepository.save(favoriteWord);


        User userInitFavor = userForFavor.get();
        userInitFavor.setFavoriteworditer(toIntExact(favoriteWord.getFavorite_words_id()));
        userRepository.save(userInitFavor);
    }

    private void sendFAQ(long chatId) {
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);
        message.setChatId(chatId);
        message.setText(FAQ);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonNewBunchFromFAQMenu = new InlineKeyboardButton();

        inlineKeyboardButtonNewBunchFromFAQMenu.setText("Start learning!");


        inlineKeyboardButtonNewBunchFromFAQMenu.setCallbackData("newbunch");

        List<InlineKeyboardButton> keyboardButtonFromFAQ = new ArrayList<>();
        keyboardButtonFromFAQ.add(inlineKeyboardButtonNewBunchFromFAQMenu);

        inlineKeyboardMarkup.setKeyboard(Collections.singletonList(keyboardButtonFromFAQ));
        message.setReplyMarkup(inlineKeyboardMarkup);
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
