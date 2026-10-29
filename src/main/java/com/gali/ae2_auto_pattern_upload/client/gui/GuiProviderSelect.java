package com.gali.ae2_auto_pattern_upload.client.gui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.moecraft.nechar.NecharUtils;

import com.gali.ae2_auto_pattern_upload.network.InstallCapacityCardPacket;
import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.upload.UploadPatternPacket;
import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import cpw.mods.fml.common.Loader;

/**
 * 供应器选择界面，移植自 1.12.2 版本，兼容 1.7.10。
 */
public class GuiProviderSelect extends GuiScreen {

    private static final int BUTTON_PREV = 100;
    private static final int BUTTON_NEXT = 101;
    private static final int BUTTON_RELOAD = 102;
    private static final int BUTTON_ADD = 103;
    private static final int BUTTON_DELETE = 104;
    private static final int BUTTON_CLOSE = 105;
    private static final int ENTRY_BUTTON_BASE = 200;
    private static final int INSTALL_CARD_BUTTON_BASE = 300;
    private static final int PAGE_SIZE = 6;

    private final GuiScreen parent;
    private List<Long> ids;
    private List<String> names;
    private List<Integer> emptySlots;
    private List<Boolean> canInstallCard;

    private final List<GroupEntry> groups = new ArrayList<>();
    private final List<GroupEntry> filtered = new ArrayList<>();

    private GuiTextField searchBox;
    private GuiTextField mappingField;
    private String query = "";
    private int page = 0;
    private boolean needsRefresh = false;
    private String lastAddedMappingName = null;
    private String lastRawRecipeId = null;

    private static class GroupEntry {

        long id;
        String name;
        int totalSlots;
        int count;
        int bestSlots;
        boolean canInstallCard; // 是否可以安装样板容量卡
        boolean pinned; // 是否置顶
    }

    // 置顶功能相关
    private static final Set<String> pinnedProviders = new HashSet<>();
    private static final String PINNED_CONFIG_PATH = "config/ae2_auto_pattern_upload/pinned_providers.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    // Component JSON 缓存相关
    private static final Map<String, String> componentCache = new HashMap<>();
    private static String lastLanguage = "";

    // 静态初始化块：加载置顶配置
    static {
        try {
            loadPinnedProviders();
        } catch (Throwable t) {
            // 加载失败时静默处理，不影响界面使用
        }
    }

    /**
     * 从配置文件加载置顶的供应器名称列表
     */
    private static synchronized void loadPinnedProviders() {
        try {
            File cfgFile = new File(PINNED_CONFIG_PATH);
            if (!cfgFile.exists()) {
                return;
            }

            JsonElement element = new JsonParser().parse(new FileReader(cfgFile));
            if (element == null || !element.isJsonObject()) {
                return;
            }

            JsonObject obj = element.getAsJsonObject();
            JsonElement pinnedElement = obj.get("pinned");
            if (pinnedElement != null && pinnedElement.isJsonArray()) {
                JsonArray arr = pinnedElement.getAsJsonArray();
                pinnedProviders.clear();
                for (JsonElement elem : arr) {
                    if (elem.isJsonPrimitive()) {
                        String name = elem.getAsString();
                        if (name != null && !name.isEmpty()) {
                            pinnedProviders.add(name);
                        }
                    }
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            // 加载失败时静默处理
        }
    }

    /**
     * 保存置顶的供应器名称列表到配置文件
     */
    private static synchronized void savePinnedProviders() {
        try {
            File cfgFile = new File(PINNED_CONFIG_PATH);
            File parentDir = cfgFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String name : pinnedProviders) {
                arr.add(new JsonPrimitive(name));
            }
            obj.add("pinned", arr);

            FileWriter writer = new FileWriter(cfgFile);
            writer.write(GSON.toJson(obj));
            writer.close();
        } catch (IOException e) {
            // 保存失败时静默处理
        }
    }

    /**
     * 将服务器发送的名称（可能是 Component JSON）反序列化为本地化文本
     */
    private String deserializeComponentName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        return componentCache.computeIfAbsent(name, k -> {
            try {
                // 如果名称是 JSON 格式的 Component，反序列化后获取本地化文本
                if (name.startsWith("{") || name.startsWith("\"")) {
                    // 1.7.10 使用 NBT JSON 格式
                    NBTBase nbt = JsonToNBT.func_150315_a(name);
                    if (nbt instanceof NBTTagCompound tag) {
                        if (tag.hasKey("text")) {
                            String text = tag.getString("text");
                            if (tag.hasKey("translate")) {
                                String translateKey = tag.getString("translate");
                                IChatComponent component = new ChatComponentTranslation(translateKey);
                                return component.getUnformattedTextForChat();
                            }
                            return text;
                        } else if (tag.hasKey("translate")) {
                            String translateKey = tag.getString("translate");
                            IChatComponent component = new ChatComponentTranslation(translateKey);
                            return component.getUnformattedTextForChat();
                        }
                    }
                }
            } catch (Exception ignored) {
                // 如果不是 JSON 或解析失败，使用原始字符串
            }
            return translatePlainName(name);
        });
    }

    private String translatePlainName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (isInvalidProviderName(name)) {
            return translate("gui.appliedenergistics2.Interface");
        }
        String translated = StatCollector.translateToLocal(name);
        if (translated == null || translated.isEmpty() || translated.equals(name)) {
            String nameKey = name + ".name";
            String nameTranslated = StatCollector.translateToLocal(nameKey);
            if (nameTranslated != null && !nameTranslated.isEmpty() && !nameTranslated.equals(nameKey)) {
                translated = nameTranslated;
            }
        }
        return translated == null || translated.isEmpty() ? name : translated;
    }

    private boolean isInvalidProviderName(String name) {
        String lower = name.trim()
            .toLowerCase(Locale.ROOT);
        return lower.isEmpty() || "nothing".equals(lower)
            || lower.startsWith("appeng.")
            || lower.startsWith("gregtech.")
            || lower.startsWith("com.")
            || lower.contains(".tile")
            || lower.contains(".part");
    }

    private static final Collator CHINESE_COLLATOR = Collator.getInstance(Locale.CHINESE);

    private static final Comparator<GroupEntry> NATURAL_SORT_COMPARATOR = new Comparator<GroupEntry>() {

        @Override
        public int compare(GroupEntry a, GroupEntry b) {
            return naturalCompare(a.name, b.name);
        }

        private int naturalCompare(String a, String b) {
            if (a == null) a = "";
            if (b == null) b = "";

            int i = 0, j = 0;
            while (i < a.length() && j < b.length()) {
                char ca = a.charAt(i);
                char cb = b.charAt(j);

                boolean aIsDigit = Character.isDigit(ca);
                boolean bIsDigit = Character.isDigit(cb);

                if (aIsDigit && bIsDigit) {
                    int numA = extractNumber(a, i);
                    int numB = extractNumber(b, j);
                    if (numA != numB) {
                        return Integer.compare(numA, numB);
                    }
                    i = skipNumber(a, i);
                    j = skipNumber(b, j);
                } else {
                    int cmp = compareChar(ca, cb);
                    if (cmp != 0) {
                        return cmp;
                    }
                    i++;
                    j++;
                }
            }
            return Integer.compare(a.length(), b.length());
        }

        private int compareChar(char ca, char cb) {
            boolean aIsChinese = isChinese(ca);
            boolean bIsChinese = isChinese(cb);

            if (aIsChinese && bIsChinese) {
                return CHINESE_COLLATOR.compare(String.valueOf(ca), String.valueOf(cb));
            }

            return Character.compare(ca, cb);
        }

        private boolean isChinese(char c) {
            return c >= 0x4E00 && c <= 0x9FA5;
        }

        private int extractNumber(String s, int start) {
            int num = 0;
            while (start < s.length() && Character.isDigit(s.charAt(start))) {
                num = num * 10 + (s.charAt(start) - '0');
                start++;
            }
            return num;
        }

        private int skipNumber(String s, int start) {
            while (start < s.length() && Character.isDigit(s.charAt(start))) {
                start++;
            }
            return start;
        }
    };

    public GuiProviderSelect(List<Long> ids, List<String> names, List<Integer> emptySlots,
        List<Boolean> canInstallCard) {
        this(null, ids, names, emptySlots, canInstallCard);
    }

    public GuiProviderSelect(GuiScreen parent, List<Long> ids, List<String> names, List<Integer> emptySlots,
        List<Boolean> canInstallCard) {
        this.parent = parent;
        this.ids = ids == null ? new ArrayList<Long>() : new ArrayList<Long>(ids);
        this.names = names == null ? new ArrayList<String>() : new ArrayList<String>(names);
        this.emptySlots = emptySlots == null ? new ArrayList<Integer>() : new ArrayList<Integer>(emptySlots);
        this.canInstallCard = canInstallCard == null ? new ArrayList<Boolean>()
            : new ArrayList<Boolean>(canInstallCard);

        String recent = RecipeNameUtil.getLastRecipeName();
        if (recent != null && !recent.isEmpty()) {
            this.query = recent;
        }
        String rawId = RecipeNameUtil.getLastRawRecipeId();
        if (rawId != null && !rawId.isEmpty()) {
            this.lastRawRecipeId = rawId;
        }
        RecipeNameUtil.clearLastRecipeName();

        buildGroups();
        applyFilter();
    }

    private void buildGroups() {
        Map<String, GroupEntry> map = new LinkedHashMap<String, GroupEntry>();
        for (int i = 0; i < names.size(); i++) {
            String rawName = names.get(i);
            // 将 Component JSON 转换为本地化文本用于分组键
            String name = deserializeComponentName(rawName);
            long id = ids.get(i);
            int slots = emptySlots.get(i);
            boolean canInstall = canInstallCard.get(i);

            GroupEntry entry = map.get(name);
            if (entry == null) {
                entry = new GroupEntry();
                entry.name = name;
                entry.pinned = pinnedProviders.contains(name);
                map.put(name, entry);
            }
            entry.count++;
            entry.totalSlots += Math.max(0, slots);
            // 只要有一个可以装卡，就标记为可以装卡
            if (canInstall) {
                entry.canInstallCard = true;
            }
            // 优先选择剩余槽位少的接口（但必须有至少1个空槽位）
            if (entry.id == 0L) {
                // 第一次初始化
                entry.bestSlots = Math.max(0, slots);
                entry.id = id;
            } else if (slots > 0 && (entry.bestSlots <= 0 || slots < entry.bestSlots)) {
                // 优先选择有槽位且剩余量少的接口
                entry.bestSlots = slots;
                entry.id = id;
            } else if (slots <= 0 && entry.bestSlots <= 0) {
                // 如果都没有空槽位，选择可以装卡的（用于显示装卡按钮）
                if (canInstall && !entry.canInstallCard) {
                    entry.bestSlots = 0;
                    entry.id = id;
                }
            }
        }
        groups.clear();
        groups.addAll(map.values());
        // 按置顶状态和自然排序排序
        groups.sort((a, b) -> {
            // 置顶的排在前面
            if (a.pinned && !b.pinned) return -1;
            if (!a.pinned && b.pinned) return 1;
            // 都置顶或都不置顶，按自然排序
            return NATURAL_SORT_COMPARATOR.compare(a, b);
        });
    }

    private void applyFilter() {
        filtered.clear();
        String q = query == null ? ""
            : query.trim()
                .toLowerCase();
        for (GroupEntry entry : groups) {
            if (q.isEmpty() || matchesSearch(entry.name, q)) {
                filtered.add(entry);
            }
        }
        if (!q.isEmpty() && filtered.isEmpty()) {
            filtered.addAll(groups);
        }
        // 按置顶状态和自然排序排序
        filtered.sort((a, b) -> {
            // 置顶的排在前面
            if (a.pinned && !b.pinned) return -1;
            if (!a.pinned && b.pinned) return 1;
            // 都置顶或都不置顶，按自然排序
            return NATURAL_SORT_COMPARATOR.compare(a, b);
        });
    }

    /**
     * 检查名称是否匹配搜索词，支持拼音搜索（如果安装了 NeverEnoughCharacters）
     */
    private boolean matchesSearch(String name, String query) {
        if (name == null || query == null) {
            return false;
        }
        String lowerName = name.toLowerCase();
        // 首先检查普通包含匹配
        if (lowerName.contains(query)) {
            return true;
        }
        // 如果安装了 NeverEnoughCharacters，使用拼音搜索
        if (Loader.isModLoaded("nechar")) {
            try {
                return NecharUtils.contain(name, query, true);
            } catch (Throwable ignored) {
                // 如果 API 调用失败，回退到普通匹配
            }
        }
        return false;
    }

    /**
     * 更新数据，保留当前搜索和页码状态
     */
    public void updateData(List<Long> newIds, List<String> newNames, List<Integer> newEmptySlots,
        List<Boolean> newCanInstallCard) {
        this.ids = newIds == null ? new ArrayList<>() : new ArrayList<>(newIds);
        this.names = newNames == null ? new ArrayList<>() : new ArrayList<>(newNames);
        this.emptySlots = newEmptySlots == null ? new ArrayList<>() : new ArrayList<>(newEmptySlots);
        this.canInstallCard = newCanInstallCard == null ? new ArrayList<>() : new ArrayList<>(newCanInstallCard);

        buildGroups();
        applyFilter();

        // 确保当前页码在有效范围内
        int totalPages = (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }
        if (page < 0) {
            page = 0;
        }

        needsRefresh = true;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        if (this.searchBox == null) {
            this.searchBox = new GuiTextField(this.fontRendererObj, centerX - 120, startY - 25, 240, 18);
            this.searchBox.setMaxStringLength(64);
        } else {
            this.searchBox.xPosition = centerX - 120;
            this.searchBox.yPosition = startY - 25;
        }
        this.searchBox.setText(query);

        int navY = startY + PAGE_SIZE * 25 + 10;
        if (this.mappingField == null) {
            this.mappingField = new GuiTextField(this.fontRendererObj, centerX - 240, navY + 30, 180, 18);
            this.mappingField.setMaxStringLength(64);
        } else {
            this.mappingField.xPosition = centerX - 240;
            this.mappingField.yPosition = navY + 30;
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());
        for (int i = start; i < end; i++) {
            int localIndex = i - start;
            GroupEntry entry = filtered.get(i);
            String label = buildLabel(entry);

            // 判断是否需要显示装卡按钮
            boolean showInstallCardBtn = entry.totalSlots <= 0 && entry.canInstallCard;

            // 主按钮宽度：如果有装卡按钮则缩短，否则保持240
            int mainBtnWidth = showInstallCardBtn ? 200 : 240;
            int mainBtnX = centerX - 120;

            GuiButton button = new GuiButton(
                ENTRY_BUTTON_BASE + localIndex,
                mainBtnX,
                startY + localIndex * 25,
                mainBtnWidth,
                20,
                label);
            // 如果没有空槽位，禁用按钮（变灰）
            if (entry.totalSlots <= 0) {
                button.enabled = false;
            }
            this.buttonList.add(button);

            // 如果可以装卡，添加安装样板容量卡按钮
            if (showInstallCardBtn) {
                GuiButton installCardBtn = new GuiButton(
                    INSTALL_CARD_BUTTON_BASE + localIndex,
                    centerX + 85,
                    startY + localIndex * 25,
                    35,
                    20,
                    translate("gui.ae2_auto_pattern_upload.install_card"));
                this.buttonList.add(installCardBtn);
            }
        }
        GuiButton prevBtn = new GuiButton(BUTTON_PREV, centerX - 60, navY, 20, 20, "<");
        GuiButton nextBtn = new GuiButton(BUTTON_NEXT, centerX + 40, navY, 20, 20, ">");
        prevBtn.enabled = page > 0;
        nextBtn.enabled = (page + 1) * PAGE_SIZE < filtered.size();
        this.buttonList.add(prevBtn);
        this.buttonList.add(nextBtn);

        GuiButton addBtn = new GuiButton(
            BUTTON_ADD,
            centerX - 50,
            navY + 30,
            50,
            20,
            translate("gui.ae2_auto_pattern_upload.add"));
        GuiButton reloadBtn = new GuiButton(
            BUTTON_RELOAD,
            centerX + 10,
            navY + 30,
            60,
            20,
            translate("gui.ae2_auto_pattern_upload.reload"));
        GuiButton delBtn = new GuiButton(
            BUTTON_DELETE,
            centerX + 80,
            navY + 30,
            50,
            20,
            translate("gui.ae2_auto_pattern_upload.delete"));
        GuiButton closeBtn = new GuiButton(BUTTON_CLOSE, centerX + 140, navY + 30, 60, 20, translate("gui.cancel"));

        this.buttonList.add(addBtn);
        this.buttonList.add(reloadBtn);
        this.buttonList.add(delBtn);
        this.buttonList.add(closeBtn);
    }

    private String buildLabel(GroupEntry entry) {
        String prefix = entry.pinned ? "★ " : "";
        return prefix + entry.name + " x" + entry.count + " - (" + entry.totalSlots + ")";
    }

    /**
     * 切换置顶状态（普通置顶）
     */
    private void togglePin(int filteredIndex) {
        if (filteredIndex < 0 || filteredIndex >= filtered.size()) {
            return;
        }
        GroupEntry entry = filtered.get(filteredIndex);
        if (entry.pinned) {
            pinnedProviders.remove(entry.name);
            entry.pinned = false;
        } else {
            pinnedProviders.add(entry.name);
            entry.pinned = true;
        }
        savePinnedProviders();
        // 重新排序
        filtered.sort((a, b) -> {
            if (a.pinned && !b.pinned) return -1;
            if (!a.pinned && b.pinned) return 1;
            return NATURAL_SORT_COMPARATOR.compare(a, b);
        });
        needsRefresh = true;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }
        int start = page * PAGE_SIZE;
        if (button.id >= ENTRY_BUTTON_BASE && button.id < ENTRY_BUTTON_BASE + PAGE_SIZE) {
            int idx = start + (button.id - ENTRY_BUTTON_BASE);
            if (idx >= 0 && idx < filtered.size()) {
                long providerId = filtered.get(idx).id;
                handleSelect(providerId);
                // handleSelect里已经处理了界面关闭，不需要重复调用
            }
            return;
        }

        // 处理安装样板容量卡按钮点击
        if (button.id >= INSTALL_CARD_BUTTON_BASE && button.id < INSTALL_CARD_BUTTON_BASE + PAGE_SIZE) {
            int idx = start + (button.id - INSTALL_CARD_BUTTON_BASE);
            if (idx >= 0 && idx < filtered.size()) {
                long providerId = filtered.get(idx).id;
                installCapacityCard(providerId);
            }
            return;
        }

        switch (button.id) {
            case BUTTON_PREV:
                changePage(-1);
                break;
            case BUTTON_NEXT:
                changePage(1);
                break;
            case BUTTON_RELOAD:
                reloadMappings();
                break;
            case BUTTON_ADD:
                addMappingFromUI();
                break;
            case BUTTON_DELETE:
                deleteMappingFromUI();
                break;
            case BUTTON_CLOSE:
                this.mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
    }

    protected void handleSelect(long providerId) {
        // 先发送上传数据包
        ModNetwork.CHANNEL.sendToServer(new UploadPatternPacket(providerId));
        // 然后关闭界面
        if (this.parent != null) {
            this.mc.displayGuiScreen(parent);
        } else {
            this.mc.displayGuiScreen(null);
        }
    }

    protected void installCapacityCard(long providerId) {
        ModNetwork.CHANNEL.sendToServer(new InstallCapacityCardPacket(providerId));
    }

    private void changePage(int delta) {
        int totalPages = (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (totalPages <= 1) {
            return;
        }
        int newPage = page + delta;
        // 循环翻页
        if (newPage < 0) {
            newPage = totalPages - 1;
        } else if (newPage >= totalPages) {
            newPage = 0;
        }
        page = newPage;
        needsRefresh = true;
    }

    private void reloadMappings() {
        RecipeNameUtil.reloadMappings();
        if (lastAddedMappingName != null && !lastAddedMappingName.isEmpty()) {
            query = lastAddedMappingName;
            page = 0;
        }
        applyFilter();
        needsRefresh = true;
        sendClientMessage(translate("ae2_auto_pattern_upload.info.mappings_reloaded"));
    }

    private void addMappingFromUI() {
        // 优先使用原始配方ID作为key，如果没有则使用搜索框内容
        String key = (lastRawRecipeId != null && !lastRawRecipeId.isEmpty()) ? lastRawRecipeId
            : (query == null ? "" : query.trim());
        String value = mappingField == null ? ""
            : mappingField.getText()
                .trim();
        if (key.isEmpty()) {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.enter_keyword"));
            return;
        }
        if (value.isEmpty()) {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.enter_mapping_name"));
            return;
        }
        if (RecipeNameUtil.addOrUpdateMapping(key, value)) {
            sendClientMessage(String.format(translate("ae2_auto_pattern_upload.info.mapping_added"), key, value));
            lastAddedMappingName = value;
            query = value; // 更新搜索框显示为新的映射名
            if (searchBox != null) {
                searchBox.setText(value);
            }
            RecipeNameUtil.reloadMappings();
            applyFilter();
            needsRefresh = true;
        } else {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.mapping_add_failed"));
        }
    }

    private void deleteMappingFromUI() {
        String value = mappingField == null ? ""
            : mappingField.getText()
                .trim();
        if (value.isEmpty()) {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.enter_mapping_delete"));
            return;
        }
        int removed = RecipeNameUtil.removeMappingsByCnValue(value);
        if (removed > 0) {
            sendClientMessage(String.format(translate("ae2_auto_pattern_upload.info.mapping_deleted"), removed));
            RecipeNameUtil.reloadMappings();
            applyFilter();
            needsRefresh = true;
        } else {
            sendClientMessage(translate("ae2_auto_pattern_upload.info.mapping_not_found"));
        }
    }

    private void sendClientMessage(String msg) {
        if (this.mc != null && this.mc.thePlayer != null && msg != null && !msg.isEmpty()) {
            this.mc.thePlayer.addChatMessage(new ChatComponentText(msg));
        }
    }

    @Override
    public void updateScreen() {
        if (searchBox != null) {
            searchBox.updateCursorCounter();
        }
        if (mappingField != null) {
            mappingField.updateCursorCounter();
        }
        if (needsRefresh) {
            needsRefresh = false;
            initGui();
        }

        // 检测语言切换
        String currentLang = Minecraft.getMinecraft().gameSettings.language;
        if (!currentLang.equals(lastLanguage)) {
            lastLanguage = currentLang;
            componentCache.clear();
            // 重新构建组以应用新的本地化名称
            buildGroups();
            applyFilter();
            needsRefresh = true;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        try {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        } catch (Exception ignored) {}
        if (searchBox != null) {
            searchBox.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mappingField != null) {
            mappingField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton == 1 && searchBox != null) {
            if (isPointInRegion(
                searchBox.xPosition,
                searchBox.yPosition,
                searchBox.width,
                searchBox.height,
                mouseX,
                mouseY)) {
                if (!searchBox.getText()
                    .isEmpty()) {
                    searchBox.setText("");
                    query = "";
                    page = 0;
                    applyFilter();
                    needsRefresh = true;
                }
                return;
            }
        }

        // 右键点击条目按钮时，切换置顶状态
        if (mouseButton == 1) {
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, filtered.size());
            int centerX = this.width / 2;
            int startY = this.height / 2 - 70;
            for (int i = start; i < end; i++) {
                int localIndex = i - start;
                // 判断是否需要显示装卡按钮
                boolean showInstallCardBtn = filtered.get(i).totalSlots <= 0 && filtered.get(i).canInstallCard;
                int mainBtnWidth = showInstallCardBtn ? 200 : 240;
                int mainBtnX = centerX - 120;
                int btnY = startY + localIndex * 25;

                if (isPointInRegion(mainBtnX, btnY, mainBtnWidth, 20, mouseX, mouseY)) {
                    togglePin(i);
                    return;
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        boolean handled = false;
        if (searchBox != null && searchBox.textboxKeyTyped(typedChar, keyCode)) {
            String newQuery = searchBox.getText();
            if (!Objects.equals(newQuery, query)) {
                query = newQuery;
                page = 0;
                applyFilter();
                needsRefresh = true;
            }
            handled = true;
        }
        if (mappingField != null && mappingField.textboxKeyTyped(typedChar, keyCode)) {
            handled = true;
        }
        // 左右箭头键翻页
        if (!handled) {
            if (keyCode == org.lwjgl.input.Keyboard.KEY_LEFT) {
                changePage(-1);
                handled = true;
            } else if (keyCode == org.lwjgl.input.Keyboard.KEY_RIGHT) {
                changePage(1);
                handled = true;
            }
        }
        if (!handled) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    private boolean isPointInRegion(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        String title = translate("ae2_auto_pattern_upload.select_provider");
        this.fontRendererObj.drawStringWithShadow(
            title,
            this.width / 2 - this.fontRendererObj.getStringWidth(title) / 2,
            this.height / 2 - 130,
            0xFFFFFF);

        if (searchBox != null) {
            searchBox.drawTextBox();
        }
        if (mappingField != null) {
            mappingField.drawTextBox();
        }

        String mappingLabel = translate("gui.ae2_auto_pattern_upload.mapping_label");
        this.fontRendererObj.drawString(
            mappingLabel,
            this.mappingField.xPosition - this.fontRendererObj.getStringWidth(mappingLabel) - 4,
            this.mappingField.yPosition + 2,
            0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) {
            if (wheel > 0) {
                // 滚轮向上 - 上一页
                changePage(-1);
            } else {
                // 滚轮向下 - 下一页
                changePage(1);
            }
        }
    }

    protected String translate(String key) {
        return StatCollector.translateToLocal(key);
    }
}
