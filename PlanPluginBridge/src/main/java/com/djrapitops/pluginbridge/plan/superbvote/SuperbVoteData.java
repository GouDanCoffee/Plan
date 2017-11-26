/* 
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.pluginbridge.plan.superbvote;

import io.minimum.minecraft.superbvote.storage.VoteStorage;
import main.java.com.djrapitops.plan.data.additional.AnalysisContainer;
import main.java.com.djrapitops.plan.data.additional.ContainerSize;
import main.java.com.djrapitops.plan.data.additional.InspectContainer;
import main.java.com.djrapitops.plan.data.additional.PluginData;
import main.java.com.djrapitops.plan.utilities.analysis.MathUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PluginData for SuperbVote plugin;
 *
 * @author Rsl1122
 */
public class SuperbVoteData extends PluginData {
    private final VoteStorage store;

    public SuperbVoteData(VoteStorage store) {
        super(ContainerSize.THIRD, "SuperbVote");
        super.setPluginIcon("check");
        super.setIconColor("teal");
        this.store = store;
    }

    @Override
    public InspectContainer getPlayerData(UUID uuid, InspectContainer inspectContainer) throws Exception {
        int votes = store.getVotes(uuid);

        inspectContainer.addValue(getWithIcon("Votes", "check", "teal"), votes);

        return inspectContainer;
    }

    @Override
    public AnalysisContainer getServerData(Collection<UUID> uuids, AnalysisContainer analysisContainer) throws Exception {
        Map<UUID, Integer> votes = new HashMap<>();
        for (UUID uuid : uuids) {
            votes.put(uuid, store.getVotes(uuid));
        }

        long totalVotes = MathUtils.sumLong(votes.values().stream().map(i -> i));
        analysisContainer.addValue(getWithIcon("Total Votes", "check", "teal"), totalVotes);

        analysisContainer.addPlayerTableValues(getWithIcon("Votes", "check"), votes);

        return analysisContainer;
    }
}