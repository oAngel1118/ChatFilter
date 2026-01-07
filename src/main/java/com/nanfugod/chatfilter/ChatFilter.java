package com.nanfugod.chatfilter;

import net.minecraft.client.Minecraft;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mod(modid = ChatFilter.MODID, version = ChatFilter.VERSION)
public class ChatFilter
{
    public static final String MODID = "ChatFilter";
    public static final String VERSION = "1.0";
    private static final HashSet<String> WHITELIST = new HashSet<>(
            Arrays.asList("[Server]", "[公告]", "[管理]", "[客服]", "[GM]", "[志愿者]")
    );
    private static final HashSet<String> IGNORE_WORDS = new HashSet<>();
    private static final HashSet<String> IGNORE_PLAYERS = new HashSet<>();

    private final Minecraft mc = Minecraft.getMinecraft();
    
    @EventHandler
    public void init(FMLPreInitializationEvent event){
        ConfigUtil.load(event);
        ChatFilterCommand.reloadConfig();
        ClientCommandHandler.instance.registerCommand(new ChatFilterCommand());
        log("loaded");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event){
        if (!ChatFilterCommand.CHAT_FILTER_ENABLED) return;
        IGNORE_WORDS.clear();
        IGNORE_PLAYERS.clear();

        String rawMessage = removeColorCodes(event.message.getUnformattedText());
        if (rawMessage.contains(":")){
            String[] split = rawMessage.split(":");
            for (String s : WHITELIST) {
                if (split[0].contains(s)) {
                    return;
                }
            }

            String playerName = filterNonAlphanumericChinese(removeBracketContent(split[0]));
            IGNORE_PLAYERS.addAll(ChatFilterCommand.TEMP_IGNORE_PLAYERS);
            IGNORE_PLAYERS.addAll(ConfigUtil.ignorePlayers);
            for (String tempIgnorePlayer : IGNORE_PLAYERS) {
                if (playerName.equalsIgnoreCase(tempIgnorePlayer)) {
                    event.setCanceled(true);
                    if (ChatFilterCommand.ENABLE_CHAT_INV) {
                        ChatStyle style = new ChatStyle();
                        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, event.message));
                        ChatComponentText iChatComponents = new ChatComponentText(ChatFilterCommand.CUSTOM_MESSAGE);
                        iChatComponents.setChatStyle(style);
                        mc.thePlayer.addChatMessage(iChatComponents);
                        return;
                    } else {
                        log("屏蔽消息：" + rawMessage + "屏蔽词：" + playerName);
                        return;
                    }
                }
            }

            String content = rawMessage.replaceAll(Pattern.quote(split[0]), "");
            content = compressSingleRepeatedChar(filterNonAlphanumericChinese(content.toLowerCase()));
            IGNORE_WORDS.addAll(ChatFilterCommand.TEMP_IGNORE_WORDS);
            IGNORE_WORDS.addAll(ConfigUtil.ignoreWords);
            for (String tempIgnoreWord : IGNORE_WORDS) {
                if (content.equalsIgnoreCase(tempIgnoreWord)) {
                    event.setCanceled(true);
                    if (ChatFilterCommand.ENABLE_CHAT_INV) {
                        ChatStyle style = new ChatStyle();
                        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, event.message));
                        ChatComponentText iChatComponents = new ChatComponentText(ChatFilterCommand.CUSTOM_MESSAGE);
                        iChatComponents.setChatStyle(style);
                        mc.thePlayer.addChatMessage(iChatComponents);
                        return;
                    }else {
                        log("屏蔽消息：" + rawMessage + "屏蔽词：" + tempIgnoreWord);
                        return;
                    }
                }
            }

        }
    }

    private static void log(String message){
        FMLLog.info("[ChatFilter] " + message);
    }

    private String removeColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "");
    }

    public static String removeBracketContent(String message) {
        if (message == null) {
            return "";
        }
        String regex = "\\[.*?\\]";
        return message.replaceAll(regex, "");
    }

    public static String filterNonAlphanumericChinese(String msg) {
        if (msg == null) {
            return "";
        }
        return msg.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_]","");
    }

    public static String compressSingleRepeatedChar(String msg) {
        if (msg == null || msg.isEmpty()) {
            return "";
        }
        if (msg.length() == 1) {
            return msg;
        }

        char baseChar = msg.charAt(0);

        for (int i = 1; i < msg.length(); i++) {
            if (msg.charAt(i) != baseChar) {
                return msg;
            }
        }

        return String.valueOf(baseChar);
    }
}
