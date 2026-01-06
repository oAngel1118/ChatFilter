package com.nanfugod.chatfilter;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;
import java.util.*;

public final class ConfigUtil {
    public static Configuration config;

    public static List<String> ignorePlayers;
    public static List<String> ignoreWords;

    /**
     * 启动初始化
     */
    public static void load(FMLPreInitializationEvent event) {
        File file = new File(event.getModConfigurationDirectory(), ChatFilter.MODID + ".cfg");
        config = new Configuration(file);

        loadInternal();
    }

    /**
     * reload配置
     */
    public static void reload() {
        if (config == null) return;
        loadInternal();
    }

    private static void loadInternal() {
        try {
            config.load();

            ignorePlayers = new ArrayList<>(
                    Arrays.asList(
                            config.getStringList(
                                    "IgnorePlayers",
                                    "filter",
                                    new String[]{"NanFuGod"},
                                    "忽略的玩家名列表"
                            )
                    )
            );

            ignoreWords = new ArrayList<>(
                    Arrays.asList(
                            config.getStringList(
                                    "IgnoreWords",
                                    "filter",
                                    new String[]{},
                                    "忽略的关键词列表"
                            )
                    )
            );

        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }


    public static boolean addIgnorePlayer(String name) {
        if (ignorePlayers.contains(name)) {
            return false;
        }

        ignorePlayers.add(name);

        config.get("filter", "IgnorePlayers", new String[0])
                .set(ignorePlayers.toArray(new String[0]));

        config.save();
        return true;
    }


    public static boolean removeIgnorePlayer(String name) {
        if (!ignorePlayers.contains(name)) {
            return false;
        }

        ignorePlayers.remove(name);

        config.get("filter", "IgnorePlayers", new String[0])
                .set(ignorePlayers.toArray(new String[0]));

        config.save();
        return true;
    }


    public static boolean addIgnoreWord(String word) {
        if (ignoreWords.contains(word.toLowerCase())) {
            return false;
        }

        ignoreWords.add(word.toLowerCase());

        config.get("filter", "IgnoreWords", new String[0])
                .set(ignoreWords.toArray(new String[0]));

        config.save();
        return true;
    }

    public static boolean removeIgnoreWord(String word) {
        if (!ignoreWords.contains(word.toLowerCase())) {
            return false;
        }

        ignoreWords.remove(word.toLowerCase());

        config.get("filter", "IgnoreWords", new String[0])
                .set(ignoreWords.toArray(new String[0]));

        config.save();
        return true;
    }
}