package com.nanfugod.chatfilter;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatFilterCommand extends CommandBase {
    public static boolean CHAT_FILTER_ENABLED = true;
    public static boolean ENABLE_CHAT_INV = true;

    protected static List<String> TEMP_IGNORE_PLAYERS = new ArrayList<>();
    protected static List<String> TEMP_IGNORE_WORDS = new ArrayList<>();

    protected static String CUSTOM_MESSAGE = "§c一条被折叠的信息，鼠标移动到此处查看";


    @Override
    public String getCommandName() {
        return "ignore";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return EnumChatFormatting.RED + "用法：\n" +
                "/ignore -reload - 重新加载聊天过滤配置\n" +
                "/ignore -add <玩家名> - 永久屏蔽玩家\n" +
                "/ignore -add \"文本\" - 永久屏蔽文本\n" +
                "/ignore <玩家名> - 临时屏蔽玩家\n" +
                "/ignore \"文本\" - 临时屏蔽文本\n" +
                "/ignore -remove <玩家名/\"文本\"> - 移除屏蔽项\n" +
                "/ignore -toggle - 切换聊天过滤总开关（开启/关闭）\n" +
                "/ignore -config <0|1> - 切换文本折叠开关（0=完全屏蔽，1=开启折叠）\n" +
                "/ignore -list - 查看所有屏蔽列表（永久/临时）";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "-reload":
                handleReload(sender);
                break;
            case "-add":
                handleAddPermanent(sender, args);
                break;
            case "-remove":
                handleRemove(sender, args);
                break;
            case "-toggle":
                handleToggle(sender);
                break;
            case "-config":
                handleConfig(sender, args);
                break;
            case "-list":
                handleList(sender);
                break;
            case "-custommsg":
                handleCustomMessage(sender, args);
                break;
            default:
                handleAddTemporary(sender, args);
                break;
        }
    }

    private void handleCustomMessage(ICommandSender sender, String[] args) {
        if (args.length >= 1) {
            CUSTOM_MESSAGE = args[1].replaceAll("&", "§");
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "已设置自定义折叠信息为：" + CUSTOM_MESSAGE));
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "当前自定义折叠信息为：" + CUSTOM_MESSAGE));
        }

    }

    /**
     * 展示所有屏蔽列表
     */
    private void handleList(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "===== [ChatFilter] 屏蔽列表 ======"));

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[永久屏蔽玩家]"));
        if (ConfigUtil.ignorePlayers.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "  无"));
        } else {
            for (int i = 0; i < ConfigUtil.ignorePlayers.size(); i++) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.WHITE + "  " + (i + 1) + ". " + ConfigUtil.ignorePlayers.get(i)
                ));
            }
        }

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[永久屏蔽文本]"));
        if (ConfigUtil.ignoreWords.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "  无"));
        } else {
            for (int i = 0; i < ConfigUtil.ignoreWords.size(); i++) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.WHITE + "  " + (i + 1) + ". 「" + ConfigUtil.ignoreWords.get(i) + "」"
                ));
            }
        }

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.BLUE + "[临时屏蔽玩家]（重启后失效）"));
        if (TEMP_IGNORE_PLAYERS.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "  无"));
        } else {
            for (int i = 0; i < TEMP_IGNORE_PLAYERS.size(); i++) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.WHITE + "  " + (i + 1) + ". " + TEMP_IGNORE_PLAYERS.get(i)
                ));
            }
        }

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.BLUE + "[临时屏蔽文本]（重启后失效）"));
        if (TEMP_IGNORE_WORDS.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "  无"));
        } else {
            for (int i = 0; i < TEMP_IGNORE_WORDS.size(); i++) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.WHITE + "  " + (i + 1) + ". 「" + TEMP_IGNORE_WORDS.get(i) + "」"
                ));
            }
        }
    }

    /**
     * 切换聊天过滤总开关
     */
    private void handleToggle(ICommandSender sender) {
        CHAT_FILTER_ENABLED = !CHAT_FILTER_ENABLED;
        String status = CHAT_FILTER_ENABLED ? "开启" : "关闭";
        sender.addChatMessage(new ChatComponentText(
                CHAT_FILTER_ENABLED? EnumChatFormatting.GREEN + "[ChatFilter] 聊天过滤总开关已" + status + "！" : EnumChatFormatting.RED + "[ChatFilter] 聊天过滤总开关已" + status + "！"
        ));
    }

    /**
     * 处理文本屏蔽子开关
     */
    private void handleConfig(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "[ChatFilter] 参数不足！用法：/ignore config <0|1>（0=关闭，1=开启）"
            ));
            return;
        }

        String param = args[1];
        if (!param.equals("0") && !param.equals("1")) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "[ChatFilter] 无效参数！仅支持 0（关闭）或 1（开启）"
            ));
            return;
        }

        ENABLE_CHAT_INV = param.equals("1");
        String status = ENABLE_CHAT_INV ? "开启" : "关闭";
        sender.addChatMessage(new ChatComponentText(
                ENABLE_CHAT_INV? EnumChatFormatting.GREEN + "[ChatFilter] 文本折叠开关已" + status + "！" : EnumChatFormatting.RED + "[ChatFilter] 文本折叠开关已" + status + "！"
        ));
    }

    /**
     * 重新加载配置
     */
    private void handleReload(ICommandSender sender) {
        try {
            reloadConfig();
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GREEN + "[ChatFilter] 配置重载成功！"));
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[ChatFilter] 配置重载失败：" + e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * 永久添加屏蔽项
     */
    private void handleAddPermanent(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "[ChatFilter] 用法：/ignore add <玩家名> 或 /ignore add \"文本\""
            ));
            return;
        }

        String target = args[1];

        if (isQuotedText(target)) {
            String word = removeQuotes(target);

            if (!ConfigUtil.addIgnoreWord(word)) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.YELLOW + "[ChatFilter] 文本「" + word + "」已在永久屏蔽列表中！"
                ));
                return;
            }

            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GREEN + "[ChatFilter] 已永久屏蔽文本：「" + word + "」"
            ));
        } else {

            if (!ConfigUtil.addIgnorePlayer(target)) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.YELLOW + "[ChatFilter] 玩家「" + target + "」已在永久屏蔽列表中！"
                ));
                return;
            }

            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GREEN + "[ChatFilter] 已永久屏蔽玩家：「" + target + "」"
            ));
        }
    }


    /**
     * 移除屏蔽项
     */
    private void handleRemove(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "[ChatFilter] 用法：/ignore remove <玩家名> 或 /ignore remove \"文本\""
            ));
            return;
        }

        String target = args[1];
        boolean isText = isQuotedText(target);
        String content = isText ? removeQuotes(target) : target;

        boolean tempRemoved;
        boolean permRemoved;

        if (isText) {
            tempRemoved = TEMP_IGNORE_WORDS.remove(content);
            permRemoved = ConfigUtil.removeIgnoreWord(content);
        } else {
            tempRemoved = TEMP_IGNORE_PLAYERS.remove(content);
            permRemoved = ConfigUtil.removeIgnorePlayer(content);
        }

        if (tempRemoved || permRemoved) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GREEN + "[ChatFilter] 已移除屏蔽：「" + content + "」"
            ));
        } else {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.YELLOW + "[ChatFilter] 未找到屏蔽项：「" + content + "」"
            ));
        }
    }


    /**
     * 临时添加屏蔽项
     */
    private void handleAddTemporary(ICommandSender sender, String[] args) {
        if (args.length > 1) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[ChatFilter] 临时屏蔽用法：/ignore <玩家名> 或 /ignore \"文本\""));
            return;
        }

        String target = args[0];
        if (isQuotedText(target)) {
            String word = removeQuotes(target);
            if (TEMP_IGNORE_WORDS.contains(word)) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[ChatFilter] 文本「" + word + "」已在临时屏蔽列表中！"));
                return;
            }
            TEMP_IGNORE_WORDS.add(word);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[ChatFilter] 已临时屏蔽文本：「" + word + "」（重启后失效）"));
        } else {
            String playerName = target;
            if (TEMP_IGNORE_PLAYERS.contains(playerName)) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[ChatFilter] 玩家「" + playerName + "」已在临时屏蔽列表中！"));
                return;
            }
            TEMP_IGNORE_PLAYERS.add(playerName);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[ChatFilter] 已临时屏蔽玩家：「" + playerName + "」（重启后失效）"));
        }
    }

    // ====================== 工具方法 ======================
    public static void reloadConfig() {
        ConfigUtil.reload();
    }

    private boolean isQuotedText(String str) {
        return (str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("“") && str.endsWith("”")) && str.length() > 2;
    }

    private String removeQuotes(String str) {
        if (isQuotedText(str)) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private List<String> getCompletableIgnoreItems() {
        List<String> completions = new ArrayList<>();
        List<String> allPlayers = new ArrayList<>();
        allPlayers.addAll(ConfigUtil.ignorePlayers);
        allPlayers.addAll(TEMP_IGNORE_PLAYERS);
        for (String player : allPlayers) {
            if (!completions.contains(player)) completions.add(player);
        }

        List<String> allWords = new ArrayList<>();
        allWords.addAll(ConfigUtil.ignoreWords);
        allWords.addAll(TEMP_IGNORE_WORDS);
        for (String word : allWords) {
            String quotedWord = "\"" + word + "\"";
            if (!completions.contains(quotedWord)) completions.add(quotedWord);
        }
        return completions;
    }

    // ====================== Tab补全（适配-list指令） ======================
    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            List<String> tabCompletions = new ArrayList<>();
            tabCompletions.addAll(getListOfStringsMatchingLastWord(args, "-toggle", "-add", "-remove", "-list", "-config", "-reload", "-customMsg"));
            tabCompletions.addAll(getOnlinePlayerNames(true));
            return getListOfStringsMatchingLastWord(args, tabCompletions);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("-add")) {
            return getListOfStringsMatchingLastWord(args, getOnlinePlayerNames(true));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("-remove")) {
            return getListOfStringsMatchingLastWord(args, getCompletableIgnoreItems());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("-config")) {
            return getListOfStringsMatchingLastWord(args, "0", "1");
        }

        if (args.length >= 2 && (args[0].equalsIgnoreCase("-toggle") || args[0].equalsIgnoreCase("-list"))) {
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }



    private List<String> getOnlinePlayerNames(boolean excludeIgnored) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.getNetHandler() == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!(player instanceof EntityOtherPlayerMP)) {
                continue;
            }

            String name = player.getName();

            if (isNpcName(name)) {
                continue;
            }

            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());

            if (info == null) {
                continue;
            }
            if (mc.thePlayer != null &&
                    name.equalsIgnoreCase(mc.thePlayer.getName())) {
                continue;
            }

            if (excludeIgnored) {
                if (ChatFilterCommand.TEMP_IGNORE_PLAYERS.contains(name)) continue;
                if (ConfigUtil.ignorePlayers.stream()
                        .anyMatch(p -> p.equalsIgnoreCase(name))) continue;
            }

            result.add(name);
        }

        return result;
    }

    private boolean isNpcName(String name) {
        if (name == null) return false;

        // 1. 长度必须是 10
        if (name.length() != 10) {
            return false;
        }

        // 2. 只能是小写字母和数字
        if (!name.matches("^[a-z0-9]+$")) {
            return false;
        }

        if (name.matches("^[a-z]+$")) {
            return false;
        }

        return !name.matches("^[0-9]+$");

    }


}