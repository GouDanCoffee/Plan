/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.java.main.java.com.djrapitops.plan;

import main.java.com.djrapitops.plan.Settings;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import test.java.utils.TestInit;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Rsl1122
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(JavaPlugin.class)
public class SettingsTest {

    public SettingsTest() {
    }

    @Before
    public void setUp() throws Exception {
        TestInit.init();
    }

    @Test
    public void testIsTrue() {
        assertTrue("COMBINE_COMMAND_ALIASES supposed to be true by default", Settings.COMBINE_COMMAND_ALIASES.isTrue());
    }

    @Test
    public void testIsTrue2() {
        Settings gatherCommands = Settings.LOG_UNKNOWN_COMMANDS;

        gatherCommands.setValue(false);
        assertFalse(gatherCommands.isTrue());

        gatherCommands.setValue(true);
        assertTrue(gatherCommands.isTrue());
    }

    @Test
    public void testToString() {
        assertEquals("SQLite", Settings.DB_TYPE.toString());
    }

    @Test
    public void testGetNumber() {
        assertEquals(8804, Settings.WEBSERVER_PORT.getNumber());
    }

    @Test
    public void testGetStringList() {
        List<String> exp = new ArrayList<>();
        exp.add("ExampleTown");
        List<String> result = Settings.HIDE_TOWNS.getStringList();
        assertEquals(exp, result);
    }

    @Test
    public void testGetPath() {
        assertEquals("WebServer.Port", Settings.WEBSERVER_PORT.getPath());
    }
}
