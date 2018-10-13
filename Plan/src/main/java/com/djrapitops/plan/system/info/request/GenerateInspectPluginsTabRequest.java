/*
 * License is provided in the jar as LICENSE also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/LICENSE
 */
package com.djrapitops.plan.system.info.request;

import com.djrapitops.plan.api.exceptions.connection.BadRequestException;
import com.djrapitops.plan.api.exceptions.connection.WebException;
import com.djrapitops.plan.system.info.InfoSystem;
import com.djrapitops.plan.system.webserver.response.DefaultResponses;
import com.djrapitops.plan.system.webserver.response.Response;
import com.djrapitops.plan.utilities.html.pages.PageFactory;
import com.djrapitops.plugin.utilities.Verify;

import java.util.Map;
import java.util.UUID;

/**
 * InfoRequest for Generating Inspect page plugins tab on receiving WebServer.
 *
 * @author Rsl1122
 */
public class GenerateInspectPluginsTabRequest extends InfoRequestWithVariables implements GenerateRequest, WideRequest {

    private final InfoSystem infoSystem;
    private final InfoRequestFactory infoRequestFactory;
    private final PageFactory pageFactory;

    private UUID playerUUID;

    GenerateInspectPluginsTabRequest(
            InfoSystem infoSystem,
            InfoRequestFactory infoRequestFactory,
            PageFactory pageFactory
    ) {
        this.infoSystem = infoSystem;
        this.infoRequestFactory = infoRequestFactory;
        this.pageFactory = pageFactory;
    }

    GenerateInspectPluginsTabRequest(
            UUID uuid,
            InfoSystem infoSystem,
            InfoRequestFactory infoRequestFactory,
            PageFactory pageFactory
    ) {
        this(infoSystem, infoRequestFactory, pageFactory);
        Verify.nullCheck(uuid);
        playerUUID = uuid;
        variables.put("player", uuid.toString());
    }

    @Override
    public Response handleRequest(Map<String, String> variables) throws WebException {
        // Available variables: sender, player

        String player = variables.get("player");
        Verify.nullCheck(player, () -> new BadRequestException("Player UUID 'player' variable not supplied in the request."));

        UUID uuid = UUID.fromString(player);

        generateAndCache(uuid);

        return DefaultResponses.SUCCESS.get();
    }

    private void generateAndCache(UUID uuid) throws WebException {
        String[] navAndHtml = pageFactory.inspectPagePluginsContent(uuid).getContents();
        infoSystem.sendRequest(infoRequestFactory.cacheInspectPluginsTabRequest(uuid, navAndHtml[0], navAndHtml[1]));
    }

    @Override
    public void runLocally() throws WebException {
        generateAndCache(playerUUID);
    }
}
