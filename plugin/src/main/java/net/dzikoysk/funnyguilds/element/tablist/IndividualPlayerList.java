package net.dzikoysk.funnyguilds.element.tablist;

import net.dzikoysk.funnyguilds.basic.rank.RankUtils;
import net.dzikoysk.funnyguilds.basic.user.User;
import net.dzikoysk.funnyguilds.element.tablist.variable.DefaultTablistVariables;
import net.dzikoysk.funnyguilds.element.tablist.variable.TablistVariablesParser;
import net.dzikoysk.funnyguilds.element.tablist.variable.VariableParsingResult;
import net.dzikoysk.funnyguilds.hook.MVdWPlaceholderAPIHook;
import net.dzikoysk.funnyguilds.hook.PlaceholderAPIHook;
import net.dzikoysk.funnyguilds.hook.PluginHook;
import net.dzikoysk.funnyguilds.nms.api.playerlist.PlayerList;
import net.dzikoysk.funnyguilds.nms.api.playerlist.PlayerListAccessor;
import net.dzikoysk.funnyguilds.nms.api.playerlist.PlayerListConstants;
import net.dzikoysk.funnyguilds.util.commons.ChatUtils;
import net.dzikoysk.funnyguilds.util.commons.MapUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Map.Entry;

public class IndividualPlayerList {
    private final User user;
    private final PlayerList playerList;
    private final TablistVariablesParser variableParser;

    private final Map<Integer, String> unformattedCells;
    private final int cellCount;
    private final String header;
    private final String footer;
    private final int cellPing;

    public IndividualPlayerList(User user,
                                PlayerListAccessor playerListAccessor,
                                Map<Integer, String> unformattedCells,
                                String header, String footer,
                                int cellPing,
                                boolean fillCells) {
        this.user = user;
        this.variableParser = new TablistVariablesParser();

        this.unformattedCells = unformattedCells;
        this.header = header;
        this.footer = footer;
        this.cellPing = cellPing;

        if (! fillCells) {
            Entry<Integer, String> entry = MapUtil.findTheMaximumEntryByKey(unformattedCells);

            if (entry != null) {
                this.cellCount = entry.getKey();
            }
            else {
                this.cellCount = PlayerListConstants.DEFAULT_CELL_COUNT;
            }
        }
        else {
            this.cellCount = PlayerListConstants.DEFAULT_CELL_COUNT;
        }

        this.playerList = playerListAccessor.createPlayerList(this.cellCount);

        DefaultTablistVariables.install(this.variableParser);
    }

    public void send() {
        String[] preparedCells = this.putVarsPrepareCells(this.unformattedCells, this.header, this.footer);
        String preparedHeader = preparedCells[PlayerListConstants.DEFAULT_CELL_COUNT];
        String preparedFooter = preparedCells[PlayerListConstants.DEFAULT_CELL_COUNT + 1];

        this.playerList.send(this.user.getPlayer(), preparedCells, preparedHeader, preparedFooter, this.cellPing);
    }

    private String[] putVarsPrepareCells(Map<Integer, String> tablistPattern, String header, String footer) {
        String[] allCells = new String[PlayerListConstants.DEFAULT_CELL_COUNT + 2]; // Additional two for header/footer
        for (int i = 0; i < this.cellCount; i++) {
            allCells[i] = this.putRank(tablistPattern.getOrDefault(i + 1, ""));
        }
        allCells[PlayerListConstants.DEFAULT_CELL_COUNT] = header;
        allCells[PlayerListConstants.DEFAULT_CELL_COUNT + 1] = footer;
        String mergedCells = StringUtils.join(allCells, '\0');
        return StringUtils.splitPreserveAllTokens(this.putVars(mergedCells), '\0');
    }

    private String putRank(String cell) {
        String temp = RankUtils.parseRank(this.user, cell);
        if (temp != null) {
            return temp;
        }
        return cell;
    }

    private String putVars(String cell) {
        String formatted = cell;

        Player player = this.user.getPlayer();

        if (player == null) {
            return formatted;
        }

        VariableParsingResult result = this.variableParser.createResultFor(this.user);
        formatted = result.replaceInString(formatted);
        formatted = ChatUtils.colored(formatted);

        if (PluginHook.isPresent(PluginHook.PLUGIN_PLACEHOLDERAPI)) {
            formatted = PlaceholderAPIHook.replacePlaceholders(player, formatted);
        }

        if (PluginHook.isPresent(PluginHook.PLUGIN_MVDWPLACEHOLDERAPI)) {
            formatted = MVdWPlaceholderAPIHook.replacePlaceholders(player, formatted);
        }

        return formatted;
    }
}
