/*
 * Copyright (c) 2019, dillydill123 <https://github.com/dillydill123>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package inventorysetups;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import inventorysetups.ui.InventorySetupsPluginPanel;
import inventorysetups.ui.InventorySetupsSlot;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import joptsimple.internal.Strings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.ScriptID;
import net.runelite.api.SpriteID;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.account.AccountSession;
import net.runelite.client.account.SessionManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.SessionClose;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.game.chatbox.ChatboxItemSearch;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.game.chatbox.ChatboxTextInput;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.RunepouchRune;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileOutputStream;


@PluginDescriptor(
	name = "Inventory Setups",
	description = "Save gear setups for specific activities"
)

@Slf4j
public class InventorySetupsPlugin extends Plugin
{

	public static final String CONFIG_GROUP = "inventorysetups";
	public static final String CONFIG_KEY_SETUPS = "setups";
	public static final String CONFIG_KEY_SECTIONS = "sections";

	public static final String CONFIG_KEY_SECTION_MODE = "sectionMode";
	public static final String CONFIG_KEY_COMPACT_MODE = "compactMode";
	public static final String CONFIG_KEY_SORTING_MODE = "sortingMode";
	public static final String CONFIG_KEY_HIDE_BUTTON = "hideHelpButton";
	public static final String CONFIG_KEY_VERSION_STR = "version";
	public static final String CONFIG_KEY_MANUAL_BANK_FILTER = "manualBankFilter";
	public static final String TUTORIAL_LINK = "https://github.com/dillydill123/inventory-setups#inventory-setups";
	public static final String SUGGESTION_LINK = "https://github.com/dillydill123/inventory-setups/issues";
	public static final int NUM_INVENTORY_ITEMS = 28;
	public static final int NUM_EQUIPMENT_ITEMS = 14;
	public static final int MAX_SETUP_NAME_LENGTH = 50;
	private static final String OPEN_SETUP_MENU_ENTRY = "Open setup";
	private static final String RETURN_TO_OVERVIEW_ENTRY = "Close current setup";
	private static final String FILTER_ADD_ITEMS_ENTRY = "Filter additional items";
	private static final String FILTER_EQUIPMENT_ENTRY = "Filter equipment";
	private static final String FILTER_INVENTORY_ENTRY = "Filter inventory";
	private static final String FILTER_ALL_ENTRY = "Filter all";
	private static final String ADD_TO_ADDITIONAL_ENTRY = "Add to Additional Filtered Items";
	private static final int SPELLBOOK_VARBIT = 4070;
	private static final int ITEMS_PER_ROW = 8;
	private static final int ITEM_VERTICAL_SPACING = 36;
	private static final int ITEM_HORIZONTAL_SPACING = 48;
	private static final int ITEM_ROW_START = 51;

	@Inject
	@Getter
	private Client client;

	@Inject
	private SessionManager sessionManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	@Getter
	private SpriteManager spriteManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	@Getter
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	@Getter
	private InventorySetupsConfig config;

	@Inject
	private Gson gson;

	@Inject
	@Getter
	private ColorPickerManager colorPickerManager;

	private InventorySetupsPluginPanel panel;

	@Getter
	private List<InventorySetup> inventorySetups;

	@Getter
	private List<InventorySetupsSection> sections;

	@Getter
	private Set<String> inventorySetupNames;

	@Getter
	private Set<String> sectionNames;

	private NavigationButton navButton;

	@Inject
	private InventorySetupsBankSearch bankSearch;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatboxItemSearch itemSearch;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	private ChatboxTextInput searchInput;

	// global filtering is allowed for any setup
	private boolean internalFilteringIsAllowed;

	// current version of the plugin
	private String currentVersion;

	@Setter
	@Getter
	private InventorySetupsFilteringModeID bankFilteringMode;

	private static final int[] RUNE_POUCH_AMOUNT_VARBITS =
		{
			Varbits.RUNE_POUCH_AMOUNT1, Varbits.RUNE_POUCH_AMOUNT2, Varbits.RUNE_POUCH_AMOUNT3
		};
	private static final int[] RUNE_POUCH_RUNE_VARBITS =
		{
			Varbits.RUNE_POUCH_RUNE1, Varbits.RUNE_POUCH_RUNE2, Varbits.RUNE_POUCH_RUNE3
		};

	private static final int BOLT_POUCH_AMOUNT1 = 2469;
	private static final int BOLT_POUCH_AMOUNT2 = 2470;
	private static final int BOLT_POUCH_AMOUNT3 = 2471;
	private static final int BOLT_POUCH_EXTRA_AMMO_AMOUNT = 2472;
	private static final int BOLT_POUCH_BOLT1 = 2473;
	private static final int BOLT_POUCH_BOLT2 = 2474;
	private static final int BOLT_POUCH_BOLT3 = 2475;
	private static final int BOLT_POUCH_EXTRA_AMMO = 2476;

	private static final int[] BOLT_POUCH_AMOUNT_VARBIT_IDS =
		{
			BOLT_POUCH_AMOUNT1, BOLT_POUCH_AMOUNT2, BOLT_POUCH_AMOUNT3, BOLT_POUCH_EXTRA_AMMO_AMOUNT
		};
	private static final int[] BOLT_POUCH_BOLT_VARBIT_IDS =
		{
			BOLT_POUCH_BOLT1, BOLT_POUCH_BOLT2, BOLT_POUCH_BOLT3, BOLT_POUCH_EXTRA_AMMO
		};

	private final HotkeyListener returnToSetupsHotkeyListener = new HotkeyListener(() -> config.returnToSetupsHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			panel.setSearchBarText("");
			panel.redrawOverviewPanel(false);
			panel.requestFocusToSearchBar();
		}
	};

	private final HotkeyListener filterBankHotkeyListener = new HotkeyListener(() -> config.filterBankHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			bankFilteringMode = InventorySetupsFilteringModeID.ALL;
			triggerBankSearchFromHotKey();
		}
	};

	private final HotkeyListener filterInventoryHotkeyListener = new HotkeyListener(() -> config.filterInventoryHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			bankFilteringMode = InventorySetupsFilteringModeID.INVENTORY;
			triggerBankSearchFromHotKey();
		}
	};

	private final HotkeyListener filterEquipmentHotkeyListener = new HotkeyListener(() -> config.filterEquipmentHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			bankFilteringMode = InventorySetupsFilteringModeID.EQUIPMENT;
			triggerBankSearchFromHotKey();
		}
	};

	private final HotkeyListener filterAddItemsHotkeyListener = new HotkeyListener(() -> config.filterAddItemsHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			bankFilteringMode = InventorySetupsFilteringModeID.ADDITIONAL_FILTERED_ITEMS;
			triggerBankSearchFromHotKey();
		}
	};

	private void triggerBankSearchFromHotKey()
	{
		// you must wait at least one game tick otherwise
		// the bank filter will work but then go back to the previous tab.
		// For some reason this can still happen but it is very rare,
		// and only when the user clicks a tab and the hot key extremely shortly after.
		int gameTick = client.getTickCount();
		clientThread.invokeLater(() ->
		{
			int gameTick2 = client.getTickCount();
			if (gameTick2 <= gameTick)
			{
				return false;
			}

			doBankSearch();
			return true;
		});
	}

	@Provides
	InventorySetupsConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InventorySetupsConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			if (event.getKey().equals(CONFIG_KEY_COMPACT_MODE) || event.getKey().equals(CONFIG_KEY_SECTION_MODE) ||
				event.getKey().equals(CONFIG_KEY_SORTING_MODE) || event.getKey().equals(CONFIG_KEY_HIDE_BUTTON))
			{
				panel.redrawOverviewPanel(false);
			}
			else if (event.getKey().equals(CONFIG_KEY_MANUAL_BANK_FILTER))
			{
				navButton.setOnClick(config.manualBankFilter() ? null : this::doBankSearch);
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		Widget bankWidget = client.getWidget(WidgetInfo.BANK_TITLE_BAR);
		if (bankWidget == null || bankWidget.isHidden())
		{
			return;
		}

		// Adds menu entries to show worn items button
		if (event.getOption().equals("Show worn items"))
		{

			@AllArgsConstructor
			class ShowWornItemsPair
			{
				InventorySetup setup;
				Integer index;
			};

			List<ShowWornItemsPair> setupsToShowOnWornItemsList;
			switch (config.showWornItemsFilter())
			{
				case BANK_FILTERED:
					setupsToShowOnWornItemsList = IntStream.range(0, inventorySetups.size())
						.mapToObj(i -> new ShowWornItemsPair(inventorySetups.get(i), i))
						.filter(i -> inventorySetups.get(i.index).isFilterBank())
						.collect(Collectors.toList());
					break;
				case FAVORITED:
					setupsToShowOnWornItemsList = IntStream.range(0, inventorySetups.size())
						.mapToObj(i -> new ShowWornItemsPair(inventorySetups.get(i), i))
						.filter(i -> inventorySetups.get(i.index).isFavorite())
						.collect(Collectors.toList());
					break;
				default:
					setupsToShowOnWornItemsList = IntStream.range(0, inventorySetups.size())
						.mapToObj(i -> new ShowWornItemsPair(inventorySetups.get(i), i))
						.collect(Collectors.toList());
					break;
			}

			for (int i = 0; i < setupsToShowOnWornItemsList.size(); i++)
			{
				final ShowWornItemsPair setupIndexPair = setupsToShowOnWornItemsList.get(setupsToShowOnWornItemsList.size() - 1 - i);
				Color menuTargetColor = setupIndexPair.setup.getDisplayColor() == null ? JagexColors.MENU_TARGET : setupIndexPair.setup.getDisplayColor();
				client.createMenuEntry(-1)
						.setOption(OPEN_SETUP_MENU_ENTRY)
						.setTarget(ColorUtil.prependColorTag(setupIndexPair.setup.getName(), menuTargetColor))
						.setIdentifier(setupIndexPair.index) // The param will used to find the correct setup if a menu entry is clicked
						.setType(MenuAction.RUNELITE)
						.onClick(e ->
						{
							resetBankSearch(true);
							panel.setCurrentInventorySetup(inventorySetups.get(setupIndexPair.index), true);
						});
			}

			if (panel.getCurrentSelectedSetup() != null)
			{
				// add menu entry to filter add items
				client.createMenuEntry(-1)
						.setOption(FILTER_ADD_ITEMS_ENTRY)
						.setType(MenuAction.RUNELITE)
						.setTarget("")
						.setIdentifier(0)
						.onClick(e -> doBankSearch(InventorySetupsFilteringModeID.ADDITIONAL_FILTERED_ITEMS));

				// add menu entry to filter equipment
				client.createMenuEntry(-1)
						.setOption(FILTER_EQUIPMENT_ENTRY)
						.setType(MenuAction.RUNELITE)
						.setTarget("")
						.setIdentifier(0)
						.onClick(e -> doBankSearch(InventorySetupsFilteringModeID.EQUIPMENT));

				// add menu entry to filter inventory
				client.createMenuEntry(-1)
						.setOption(FILTER_INVENTORY_ENTRY)
						.setType(MenuAction.RUNELITE)
						.setTarget("")
						.setIdentifier(0)
						.onClick(e -> doBankSearch(InventorySetupsFilteringModeID.INVENTORY));

				// add menu entry to filter all
				client.createMenuEntry(-1)
						.setOption(FILTER_ALL_ENTRY)
						.setType(MenuAction.RUNELITE)
						.setTarget("")
						.setIdentifier(0)
						.onClick(e -> doBankSearch(InventorySetupsFilteringModeID.ALL));

				// add menu entry to close setup
				client.createMenuEntry(-1)
						.setOption(RETURN_TO_OVERVIEW_ENTRY)
						.setType(MenuAction.RUNELITE)
						.setTarget("")
						.setIdentifier(0)
						.onClick(e -> panel.returnToOverviewPanel(false));
			}
		}
		// If shift is held and item is right clicked in the bank while a setup is active,
		// add item to additional filtered items
		else if (panel.getCurrentSelectedSetup() != null
			&& event.getActionParam1() == WidgetInfo.BANK_ITEM_CONTAINER.getId()
			&& client.isKeyPressed(KeyCode.KC_SHIFT)
			&& event.getOption().equals("Examine"))
		{
			client.createMenuEntry(-1)
					.setOption(ADD_TO_ADDITIONAL_ENTRY)
					.setType(MenuAction.RUNELITE)
					.setTarget("")
					.setIdentifier(0)
					.setParam0(event.getActionParam0())
					.setParam1(event.getActionParam1())
					.onClick(e ->
					{
						final Item newItem = retrieveItemFromBankMenuEntry(e.getParam0());
						if (newItem == null)
						{
							return;
						}

						final Map<Integer, InventorySetupsItem> additionalFilteredItems =
								panel.getCurrentSelectedSetup().getAdditionalFilteredItems();

						// Item already exists, don't add it again
						if (!additionalFilteredItemsHasItem(newItem.getId(), additionalFilteredItems))
						{
							addAdditionalFilteredItem(newItem.getId(), additionalFilteredItems);
						}
					});
		}
	}

	// Retrieve an item from a selected menu entry in the bank
	private Item retrieveItemFromBankMenuEntry(int inventoryIndex)
	{
		// This should never be hit, as the option only appears when the panel isn't null
		if (panel.getCurrentSelectedSetup() == null)
		{
			return null;
		}

		ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);
		if (bankContainer == null)
		{
			return null;
		}
		Item[] items = bankContainer.getItems();
		if (inventoryIndex < 0 || inventoryIndex >= items.length)
		{
			return null;
		}
		return bankContainer.getItems()[inventoryIndex];
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// when the bank is loaded up allowing filtering again
		// this is to make it so the bank will refilter if a tab was clicked and then the player exited the bank
		if (event.getGroupId() == WidgetID.BANK_GROUP_ID)
		{

			// If manual bank filter is selected, don't allow filtering when the bank is opened
			// filtering will only occur if the user selects a setup or uses a filtering hotkey
			// while the bank is already open
			internalFilteringIsAllowed = !config.manualBankFilter();

			if (panel.getCurrentSelectedSetup() != null && panel.getCurrentSelectedSetup().isFilterBank() && isFilteringAllowed())
			{
				// start a bank search so the bank is filtered when it's opened
				doBankSearch();
			}
		}
	}

	public void switchViews(final String mode, boolean on)
	{
		configManager.setConfiguration(CONFIG_GROUP, mode, on);
	}

	public void toggleAlphabeticalMode(InventorySetupsSortingID mode)
	{
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_SORTING_MODE, mode);
	}

	public String getSavedVersionString()
	{
		final String versionStr = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_VERSION_STR);
		return versionStr == null ? "" : versionStr;
	}

	public void setSavedVersionString(final String newVersion)
	{
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_VERSION_STR, newVersion);
	}

	public String getCurrentVersionString()
	{
		return currentVersion;
	}

	@Override
	public void startUp()
	{
		// get current version of the plugin using properties file generated by build.gradle
		try
		{
			final Properties props = new Properties();
			InputStream is = InventorySetupsPlugin.class.getResourceAsStream("/version.txt");
			props.load(is);
			this.currentVersion = props.getProperty("version");
		}
		catch (Exception e)
		{
			log.warn("Could not determine current plugin version", e);
			this.currentVersion = "";
		}

		this.internalFilteringIsAllowed = true;
		this.panel = new InventorySetupsPluginPanel(this, itemManager);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/inventorysetups_icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Inventory Setups")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();

		// Clicking the nav button will do a bank search
		navButton.setOnClick(config.manualBankFilter() ? null : this::doBankSearch);

		clientToolbar.addNavigation(navButton);
		keyManager.registerKeyListener(returnToSetupsHotkeyListener);
		keyManager.registerKeyListener(filterBankHotkeyListener);
		keyManager.registerKeyListener(filterInventoryHotkeyListener);
		keyManager.registerKeyListener(filterEquipmentHotkeyListener);
		keyManager.registerKeyListener(filterAddItemsHotkeyListener);

		bankFilteringMode = InventorySetupsFilteringModeID.ALL;

		this.gson = this.gson.newBuilder().registerTypeAdapter(long.class, new LongTypeAdapter()).create();

		// load all the inventory setups from the config file
		clientThread.invokeLater(() ->
		{
			switch (client.getGameState())
			{
				case STARTING:
				case UNKNOWN:
					return false;
			}

			loadConfig();

			return true;
		});

	}

	public void addInventorySetup()
	{
		final String msg = "Enter the name of this setup (max " + MAX_SETUP_NAME_LENGTH + " chars).";
		String name = JOptionPane.showInputDialog(panel,
			msg,
			"Add New Setup",
			JOptionPane.PLAIN_MESSAGE);

		// cancel button was clicked
		if (name == null || name.isEmpty())
		{
			return;
		}

		if (name.length() > MAX_SETUP_NAME_LENGTH)
		{
			name = name.substring(0, MAX_SETUP_NAME_LENGTH);
		}

		if (inventorySetupNames.contains(name))
		{
			JOptionPane.showMessageDialog(panel,
					"A setup with the name " + name + " already exists",
					"Setup Already Exists",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		final String newName = name;

		clientThread.invokeLater(() ->
		{
			List<InventorySetupsItem> inv = getNormalizedContainer(InventoryID.INVENTORY);
			List<InventorySetupsItem> eqp = getNormalizedContainer(InventoryID.EQUIPMENT);

			List<InventorySetupsItem> runePouchData = null;
			final boolean inventoryHasRunePouch = containerContainsRunePouch(inv);
			List<InventorySetupsItem> boltPouchData = null;
			final boolean inventoryHasBoltPouch = containerContainsBoltPouch(inv);

			if (inventoryHasRunePouch)
			{
				runePouchData = getRunePouchData();
			}

			if (inventoryHasBoltPouch)
			{
				boltPouchData = getBoltPouchData();
			}

			int spellbook = getCurrentSpellbook();

			final InventorySetup invSetup = new InventorySetup(inv, eqp, runePouchData, boltPouchData, new HashMap<>(), newName, "",
				config.highlightColor(),
				config.highlightDifference(),
				config.enableDisplayColor() ? config.displayColor() : null,
				config.bankFilter(),
				config.highlightUnorderedDifference(),
				spellbook, false);

			inventorySetups.add(invSetup);
			inventorySetupNames.add(invSetup.getName());
			updateConfig(true, false);
			SwingUtilities.invokeLater(() -> panel.redrawOverviewPanel(false));

		});
	}

	public void addSection()
	{
		final String msg = "Enter the name of this section (max " + MAX_SETUP_NAME_LENGTH + " chars).";
		String name = JOptionPane.showInputDialog(panel,
				msg,
				"Add New Setup",
				JOptionPane.PLAIN_MESSAGE);

		// cancel button was clicked
		if (name == null || name.isEmpty())
		{
			return;
		}

		if (name.length() > MAX_SETUP_NAME_LENGTH)
		{
			name = name.substring(0, MAX_SETUP_NAME_LENGTH);
		}

		if (sectionNames.contains(name))
		{
			JOptionPane.showMessageDialog(panel,
					"A section with the name " + name + " already exists",
					"Section Already Exists",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		final String newName = name;
		InventorySetupsSection newSection = new InventorySetupsSection(newName);
		sections.add(newSection);
		sectionNames.add(newSection.getName());

		updateConfig(false, true);
		SwingUtilities.invokeLater(() -> panel.redrawOverviewPanel(false));

	}

	public void addSetupToSections(final InventorySetup setup, final List<String> sectionNames)
	{
		for (final String sectionName : sectionNames)
		{
			// Get the appropriate section
			final InventorySetupsSection section = sections.stream().filter(s -> s.getName().equals(sectionName)).findFirst().get();
			// Don't add the setup if it's already part of a section
			if (!section.getSetups().contains(setup.getName()))
			{
				section.getSetups().add(setup.getName());
			}
		}

		updateConfig(false, true);
		panel.redrawOverviewPanel(false);
	}

	public void addSetupsToSection(final InventorySetupsSection section, final List<String> setupNames)
	{
		for (final String setupName : setupNames)
		{
			if (!section.getSetups().contains(setupName))
			{
				section.getSetups().add(setupName);
			}
		}
		updateConfig(false, true);
		panel.redrawOverviewPanel(false);
	}

	public void moveSetup(int invIndex, int newPosition)
	{
		// Setup is already in the specified position or is out of position
		if (isNewPositionInvalid(invIndex, newPosition, inventorySetups.size()))
		{
			return;
		}
		InventorySetup setup = inventorySetups.remove(invIndex);
		inventorySetups.add(newPosition, setup);
		panel.redrawOverviewPanel(false);
		updateConfig(true, false);
	}

	public void moveSection(int sectionIndex, int newPosition)
	{
		// Setup is already in the specified position or is out of position
		if (isNewPositionInvalid(sectionIndex, newPosition, sections.size()))
		{
			return;
		}
		InventorySetupsSection section = sections.remove(sectionIndex);
		sections.add(newPosition, section);
		panel.redrawOverviewPanel(false);
		updateConfig(false, true);
	}

	private boolean isNewPositionInvalid(int oldPosition, int newPosition, int size)
	{
		return oldPosition == newPosition || newPosition < 0 || newPosition >= size;
	}

	public List<InventorySetup> filterSetups(String textToFilter)
	{
		final String textToFilterLower = textToFilter.toLowerCase();
		return inventorySetups.stream()
			.filter(i -> i.getName().toLowerCase().contains(textToFilterLower))
			.collect(Collectors.toList());
	}

	public void doBankSearch(final InventorySetupsFilteringModeID filteringModeID)
	{
		bankFilteringMode = filteringModeID;
		doBankSearch();
	}

	public void doBankSearch()
	{
		final InventorySetup currentSelectedSetup = panel.getCurrentSelectedSetup();
		internalFilteringIsAllowed = true;

		if (currentSelectedSetup != null && currentSelectedSetup.isFilterBank())
		{

			clientThread.invoke(() ->
			{
				client.setVarbit(Varbits.CURRENT_BANK_TAB, 0);
				bankSearch.layoutBank();

				// When tab is selected with search window open, the search window closes but the search button
				// stays highlighted, this solves that issue
				Widget bankContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
				if (bankContainer != null && !bankContainer.isHidden())
				{
					Widget searchBackground = client.getWidget(WidgetInfo.BANK_SEARCH_BUTTON_BACKGROUND);
					searchBackground.setSpriteId(SpriteID.EQUIPMENT_SLOT_TILE);
				}
			});
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{

		if (event.getMenuAction() == MenuAction.RUNELITE)
		{
			return;
		}

		if (panel.getCurrentSelectedSetup() == null)
		{
			return;
		}

		if (event.getParam1() == WidgetInfo.BANK_ITEM_CONTAINER.getId() && event.getMenuOption().startsWith("View tab"))
		{
			if (config.disableBankTabBar())
			{
				event.consume();
			}
		}
		else if (panel.getCurrentSelectedSetup() != null
			&& (event.getMenuOption().startsWith("View tab") || event.getMenuOption().equals("View all items")))
		{
			internalFilteringIsAllowed = false;
		}
	}

	private boolean additionalFilteredItemsHasItem(int itemId, final Map<Integer, InventorySetupsItem> additionalFilteredItems)
	{
		final int canonicalizedId = itemManager.canonicalize(itemId);
		for (final Integer additionalItemKey : additionalFilteredItems.keySet())
		{
			boolean isFuzzy = additionalFilteredItems.get(additionalItemKey).isFuzzy();
			int addItemId = getProcessedID(isFuzzy, additionalFilteredItems.get(additionalItemKey).getId());
			int finalItemId = getProcessedID(isFuzzy, canonicalizedId);
			if (addItemId == finalItemId)
			{
				return true;
			}
		}
		return false;
	}

	private void addAdditionalFilteredItem(int itemId, final Map<Integer, InventorySetupsItem> additionalFilteredItems)
	{
		// un-noted, un-placeholdered ID
		final int processedItemId = itemManager.canonicalize(itemId);

		clientThread.invokeLater(() ->
		{
			final String name = itemManager.getItemComposition(processedItemId).getName();
			InventorySetupsStackCompareID stackCompareType = panel.isStackCompareForSlotAllowed(InventorySetupsSlotID.ADDITIONAL_ITEMS, 0) ? config.stackCompareType() : InventorySetupsStackCompareID.None;
			final InventorySetupsItem setupItem = new InventorySetupsItem(processedItemId, name, 1, config.fuzzy(), stackCompareType);

			additionalFilteredItems.put(processedItemId, setupItem);
			updateConfig(true, false);
			panel.refreshCurrentSetup();
		});
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{

		if (event.getIndex() == 439 && client.getGameState() == GameState.LOGGED_IN)
		{
			// must be invoked later otherwise causes freezing.
			clientThread.invokeLater(() ->
			{
				panel.highlightSpellbook();
			});
		}

	}

	public void resetBankSearch(boolean closeChat)
	{
		// Only reset the bank automatically if filtering is allowed
		// This makes it so that you click the search button again to cancel a filter
		if (isFilteringAllowed())
		{
			bankSearch.reset(closeChat);
		}
	}

	public List<InventorySetupsItem> getRunePouchData()
	{
		List<InventorySetupsItem> runePouchData = new ArrayList<>();

		for (int i = 0; i < RUNE_POUCH_RUNE_VARBITS.length; i++)
		{
			int runeId = client.getVarbitValue(RUNE_POUCH_RUNE_VARBITS[i]);
			RunepouchRune rune = RunepouchRune.getRune(runeId);
			int runeAmount = rune == null ? 0 : client.getVarbitValue(RUNE_POUCH_AMOUNT_VARBITS[i]);
			String runeName = rune == null ? "" : rune.getName();
			int runeItemId = rune == null ? -1 : rune.getItemId();

			InventorySetupsStackCompareID stackCompareType = panel.isStackCompareForSlotAllowed(InventorySetupsSlotID.RUNE_POUCH, i) ? config.stackCompareType() : InventorySetupsStackCompareID.None;
			runePouchData.add(new InventorySetupsItem(runeItemId, runeName, runeAmount, false, stackCompareType));
		}

		return runePouchData;
	}

	public List<InventorySetupsItem> getBoltPouchData()
	{
		List<InventorySetupsItem> boltPouchData = new ArrayList<>();

		for (int i = 0; i < BOLT_POUCH_BOLT_VARBIT_IDS.length; i++)
		{
			int boltVarbitId = client.getVarbitValue(BOLT_POUCH_BOLT_VARBIT_IDS[i]);
			Bolts bolt = Bolts.getBolt(boltVarbitId);
			boolean boltNotFound = bolt == null;
			int boltAmount = boltNotFound ? 0 : client.getVarbitValue(BOLT_POUCH_AMOUNT_VARBIT_IDS[i]);
			String boltName = boltNotFound ? "" : itemManager.getItemComposition(bolt.getItemId()).getName();
			int boltItemId = boltNotFound ? -1 : bolt.getItemId();

			InventorySetupsStackCompareID stackCompareType =
				panel.isStackCompareForSlotAllowed(InventorySetupsSlotID.BOLT_POUCH, i)
					? config.stackCompareType() : InventorySetupsStackCompareID.None;
			boltPouchData.add(new InventorySetupsItem(boltItemId, boltName, boltAmount, false, stackCompareType));
		}

		return boltPouchData;
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		String eventName = event.getEventName();

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();

		switch (eventName)
		{
			case "bankSearchFilter":
			{
				final InventorySetup currentSetup = panel.getCurrentSelectedSetup();
				// Shared storage uses the bankmain filter scripts too. Allow using tag searches in it but don't
				// apply the tag search from the active tab.
				final boolean bankOpen = client.getItemContainer(InventoryID.BANK) != null;
				if (bankOpen && currentSetup != null && currentSetup.isFilterBank() && isFilteringAllowed())
				{
					int itemId = intStack[intStackSize - 1];
					boolean containsItem = false;
					switch (bankFilteringMode)
					{
						case ALL:
							containsItem = setupContainsItem(currentSetup, itemId);
							break;
						case INVENTORY:
							boolean runePouchContainsItem = false;
							if (currentSetup.getRune_pouch() != null)
							{
								runePouchContainsItem = checkIfContainerContainsItem(itemId, currentSetup.getRune_pouch());
							}
							boolean boltPouchContainsItem = false;
							if (currentSetup.getBoltPouch() != null)
							{
								boltPouchContainsItem = checkIfContainerContainsItem(itemId, currentSetup.getBoltPouch());
							}
							containsItem = runePouchContainsItem || boltPouchContainsItem ||
								checkIfContainerContainsItem(itemId, currentSetup.getInventory());
							break;
						case EQUIPMENT:
							containsItem = checkIfContainerContainsItem(itemId, currentSetup.getEquipment());
							break;
						case ADDITIONAL_FILTERED_ITEMS:
							containsItem = additionalFilteredItemsHasItem(itemId, currentSetup.getAdditionalFilteredItems());
							break;
					}
					if (containsItem)
					{
						// return true
						intStack[intStackSize - 2] = 1;
					}
					else
					{
						intStack[intStackSize - 2] = 0;
					}
				}
				break;
			}
			case "getSearchingTagTab":
				// Clicking on a bank tab that isn't the first one (main tab),
				// then filtering the bank (either by selecting a setup or hotkey),
				// then clicking on "item" or "note" would cause the bank to show the tab
				// and remove the filter. This stops this from happening.
				final InventorySetup currentSetup = panel.getCurrentSelectedSetup();
				if (currentSetup != null && currentSetup.isFilterBank() && isFilteringAllowed())
				{
					intStack[intStackSize - 1] = 1;
				}
				else
				{
					intStack[intStackSize - 1] = 0;
				}
				break;
		}


	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING)
		{
			// Bankmain_build will reset the bank title to "The Bank of Gielinor". So apply our own title.
			if (panel.getCurrentSelectedSetup() != null && panel.getCurrentSelectedSetup().isFilterBank() && isFilteringAllowed())
			{
				String postTitle = " - ";
				switch (bankFilteringMode)
				{
					case ALL:
						postTitle += "All Items";
						break;
					case INVENTORY:
						postTitle += "Inventory";
						break;
					case EQUIPMENT:
						postTitle += "Equipment";
						break;
					case ADDITIONAL_FILTERED_ITEMS:
						postTitle += "Additional Items";
						break;
				}
				Widget bankTitle = client.getWidget(WidgetInfo.BANK_TITLE_BAR);
				bankTitle.setText("Inventory Setup <col=ff0000>" + panel.getCurrentSelectedSetup().getName() + postTitle + "</col>");
			}
		}
		else if (event.getScriptId() == ScriptID.BANKMAIN_SEARCH_TOGGLE)
		{
			// cancel the current filtering if the search button is clicked
			resetBankSearch(true);

			// don't allow the bank to retry a filter if the search button is clicked
			internalFilteringIsAllowed = false;
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_SEARCHING)
		{
			// The return value of bankmain_searching is on the stack. If we have a setup active
			// make it return true to put the bank in a searching state.
			boolean bankOpen = client.getItemContainer(InventoryID.BANK) != null;
			if (bankOpen && panel.getCurrentSelectedSetup() != null && panel.getCurrentSelectedSetup().isFilterBank() && isFilteringAllowed())
			{
				client.getIntStack()[client.getIntStackSize() - 1] = 1; // true
			}
		}

		if (event.getScriptId() != ScriptID.BANKMAIN_BUILD)
		{
			return;
		}

		int items = 0;

		Widget itemContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		if (itemContainer == null)
		{
			return;
		}

		if (panel.getCurrentSelectedSetup() != null && config.removeBankTabSeparator() && isFilteringAllowed())
		{
			Widget[] containerChildren = itemContainer.getDynamicChildren();

			// sort the child array as the items are not in the displayed order
			Arrays.sort(containerChildren, Comparator.comparing(Widget::getOriginalY).thenComparing(Widget::getOriginalX));

			for (Widget child : containerChildren)
			{
				if (child.getItemId() != -1 && !child.isHidden())
				{
					// calculate correct item position as if this was a normal tab
					int adjYOffset = (items / ITEMS_PER_ROW) * ITEM_VERTICAL_SPACING;
					int adjXOffset = (items % ITEMS_PER_ROW) * ITEM_HORIZONTAL_SPACING + ITEM_ROW_START;

					if (child.getOriginalY() != adjYOffset)
					{
						child.setOriginalY(adjYOffset);
						child.revalidate();
					}

					if (child.getOriginalX() != adjXOffset)
					{
						child.setOriginalX(adjXOffset);
						child.revalidate();
					}

					items++;
				}

				// separator line or tab text
				if (child.getSpriteId() == SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND || child.getText().contains("Tab"))
				{
					child.setHidden(true);
				}
			}
		}

	}

	public void updateCurrentSetup(InventorySetup setup)
	{
		int confirm = JOptionPane.showConfirmDialog(panel,
			"Are you sure you want update this inventory setup?",
			"Warning", JOptionPane.OK_CANCEL_OPTION);

		// cancel button was clicked
		if (confirm != JOptionPane.YES_OPTION)
		{
			return;
		}

		// must be on client thread to get names
		clientThread.invokeLater(() ->
		{
			List<InventorySetupsItem> inv = getNormalizedContainer(InventoryID.INVENTORY);
			List<InventorySetupsItem> eqp = getNormalizedContainer(InventoryID.EQUIPMENT);

			// copy over fuzzy attributes
			for (int i = 0; i < inv.size(); i++)
			{
				inv.get(i).setFuzzy(setup.getInventory().get(i).isFuzzy());
				inv.get(i).setStackCompare(setup.getInventory().get(i).getStackCompare());
			}
			for (int i = 0; i < eqp.size(); i++)
			{
				eqp.get(i).setFuzzy(setup.getEquipment().get(i).isFuzzy());
				eqp.get(i).setStackCompare(setup.getEquipment().get(i).getStackCompare());
			}

			List<InventorySetupsItem> runePouchData = null;
			if (checkIfContainerContainsItem(ItemID.RUNE_POUCH, inv))
			{
				runePouchData = getRunePouchData();
			}

			List<InventorySetupsItem> boltPouchData = null;
			if (checkIfContainerContainsItem(ItemID.BOLT_POUCH, inv))
			{
				boltPouchData = getBoltPouchData();
			}

			setup.updateRunePouch(runePouchData);
			setup.updateBoltPouch(boltPouchData);
			setup.updateInventory(inv);
			setup.updateEquipment(eqp);
			setup.updateSpellbook(getCurrentSpellbook());
			updateConfig(true, false);
			panel.refreshCurrentSetup();
		});
	}

	public void updateSlotFromContainer(final InventorySetupsSlot slot)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			JOptionPane.showMessageDialog(panel,
				"You must be logged in to update from " + (slot.getSlotID().toString().toLowerCase() + "."),
				"Cannot Update Item",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		final List<InventorySetupsItem> container = getContainerFromSlot(slot);
		final boolean isFuzzy = getContainerFromSlot(slot).get(slot.getIndexInSlot()).isFuzzy();
		final InventorySetupsStackCompareID stackCompareType = getContainerFromSlot(slot).get(slot.getIndexInSlot()).getStackCompare();

		// must be invoked on client thread to get the name
		clientThread.invokeLater(() ->
		{
			final List<InventorySetupsItem> playerContainer = getNormalizedContainer(slot.getSlotID());
			final InventorySetupsItem newItem = playerContainer.get(slot.getIndexInSlot());
			newItem.setFuzzy(isFuzzy);
			newItem.setStackCompare(stackCompareType);

			// update the rune pouch data
			if (!checkAndUpdateSlotIfRunePouchWasSelected(slot, container.get(slot.getIndexInSlot()), newItem))
			{
				return;
			}

			// update the bolt pouch data
			if (!checkAndUpdateSlotIfBoltPouchWasSelected(slot, container.get(slot.getIndexInSlot()), newItem))
			{
				return;
			}

			container.set(slot.getIndexInSlot(), newItem);
			updateConfig(true, false);
			panel.refreshCurrentSetup();
		});

	}

	public void updateSlotFromSearch(final InventorySetupsSlot slot, boolean allowStackable)
	{

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			JOptionPane.showMessageDialog(panel,
				"You must be logged in to search.",
				"Cannot Search for Item",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		itemSearch
			.tooltipText("Set slot to")
			.onItemSelected((itemId) ->
			{
				clientThread.invokeLater(() ->
				{
					int finalId = itemManager.canonicalize(itemId);

					// NOTE: the itemSearch shows items from skill guides which can be selected, which may be highlighted

					// if the item is stackable, ask for a quantity
					if (allowStackable && itemManager.getItemComposition(finalId).isStackable())
					{
						final int finalIdCopy = finalId;
						searchInput = chatboxPanelManager.openTextInput("Enter amount")
							// only allow numbers and k, m, b (if 1 value is available)
							// stop once k, m, or b is seen
							.addCharValidator(this::validateCharFromItemSearch)
							.onDone((input) ->
							{
								clientThread.invokeLater(() ->
								{
									int quantity = parseTextInputAmount(input);

									final List<InventorySetupsItem> container = getContainerFromSlot(slot);
									final String itemName = itemManager.getItemComposition(finalIdCopy).getName();
									final InventorySetupsItem itemToBeReplaced = container.get(slot.getIndexInSlot());
									final InventorySetupsItem newItem = new InventorySetupsItem(finalIdCopy, itemName, quantity, itemToBeReplaced.isFuzzy(), itemToBeReplaced.getStackCompare());

									// update the rune pouch data
									if (!checkAndUpdateSlotIfRunePouchWasSelected(slot, container.get(slot.getIndexInSlot()), newItem))
									{
										return;
									}

									// update the bolt pouch data
									if (!checkAndUpdateSlotIfBoltPouchWasSelected(slot, container.get(slot.getIndexInSlot()), newItem))
									{
										return;
									}

									container.set(slot.getIndexInSlot(), newItem);
									updateConfig(true, false);
									panel.refreshCurrentSetup();

								});
							}).build();
					}
					else
					{
						if (slot.getSlotID() == InventorySetupsSlotID.ADDITIONAL_ITEMS)
						{
							final Map<Integer, InventorySetupsItem> additionalFilteredItems =
								panel.getCurrentSelectedSetup().getAdditionalFilteredItems();
							if (!additionalFilteredItemsHasItem(finalId, additionalFilteredItems))
							{
								removeAdditionalFilteredItem(slot, additionalFilteredItems);
								addAdditionalFilteredItem(finalId, additionalFilteredItems);
								// duplicate update config and refresh setup are being called here
							}
						}
						else
						{
							final List<InventorySetupsItem> container = getContainerFromSlot(slot);
							final String itemName = itemManager.getItemComposition(finalId).getName();
							final InventorySetupsItem itemToBeReplaced = container.get(slot.getIndexInSlot());
							final InventorySetupsItem newItem = new InventorySetupsItem(finalId, itemName, 1, itemToBeReplaced.isFuzzy(), itemToBeReplaced.getStackCompare());
							// update the rune pouch data
							if (!checkAndUpdateSlotIfRunePouchWasSelected(slot, container.get(slot.getIndexInSlot()), newItem))
							{
								return;
							}
							// update the bolt pouch data
							if (!checkAndUpdateSlotIfBoltPouchWasSelected(slot, container.get(slot.getIndexInSlot()), newItem))
							{
								return;
							}
							container.set(slot.getIndexInSlot(), newItem);
						}

						updateConfig(true, false);
						panel.refreshCurrentSetup();
					}

				});
			})
			.build();
	}

	private boolean validateCharFromItemSearch(int arg)
	{
		// allow more numbers to be put in if a letter hasn't been detected
		boolean stillInputtingNumbers = arg >= '0' && arg <= '9' &&
			!searchInput.getValue().toLowerCase().contains("k") &&
			!searchInput.getValue().toLowerCase().contains("m") &&
			!searchInput.getValue().toLowerCase().contains("b");

		// if a letter is input, check if there isn't one already and the length is not 0
		boolean letterIsInput = (arg == 'b' || arg == 'B' ||
				arg == 'k' || arg == 'K' ||
				arg == 'm' || arg == 'M') &&
				searchInput.getValue().length() > 0 &&
				!searchInput.getValue().toLowerCase().contains("k") &&
				!searchInput.getValue().toLowerCase().contains("m") &&
				!searchInput.getValue().toLowerCase().contains("b");

		return stillInputtingNumbers || letterIsInput;
	}

	public void removeItemFromSlot(final InventorySetupsSlot slot)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			JOptionPane.showMessageDialog(panel,
				"You must be logged in to remove item from the slot.",
				"Cannot Remove Item",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// must be invoked on client thread to get the name
		clientThread.invokeLater(() ->
		{

			if (slot.getSlotID() == InventorySetupsSlotID.ADDITIONAL_ITEMS)
			{
				removeAdditionalFilteredItem(slot, panel.getCurrentSelectedSetup().getAdditionalFilteredItems());
				updateConfig(true, false);
				panel.refreshCurrentSetup();
				return;
			}

			final List<InventorySetupsItem> container = getContainerFromSlot(slot);

			// update the rune pouch data
			final InventorySetupsItem itemToBeReplaced = container.get(slot.getIndexInSlot());
			final InventorySetupsItem dummyItem = new InventorySetupsItem(-1, "", 0, itemToBeReplaced.isFuzzy(), itemToBeReplaced.getStackCompare());
			if (!checkAndUpdateSlotIfRunePouchWasSelected(slot, container.get(slot.getIndexInSlot()), dummyItem))
			{
				return;
			}

			// update the bolt pouch data
			if (!checkAndUpdateSlotIfBoltPouchWasSelected(slot, container.get(slot.getIndexInSlot()), dummyItem))
			{
				return;
			}

			container.set(slot.getIndexInSlot(), dummyItem);
			updateConfig(true, false);
			panel.refreshCurrentSetup();
		});
	}

	public void toggleFuzzyOnSlot(final InventorySetupsSlot slot)
	{
		if (panel.getCurrentSelectedSetup() == null)
		{
			return;
		}

		if (slot.getSlotID() == InventorySetupsSlotID.ADDITIONAL_ITEMS)
		{
			final Map<Integer, InventorySetupsItem> additionalFilteredItems = slot.getParentSetup().getAdditionalFilteredItems();
			final int slotID = slot.getIndexInSlot();
			int j = 0;
			Integer keyToMakeFuzzy = null;
			for (final Integer key : additionalFilteredItems.keySet())
			{
				if (slotID == j)
				{
					keyToMakeFuzzy = key;
					break;
				}
				j++;
			}
			additionalFilteredItems.get(keyToMakeFuzzy).toggleIsFuzzy();
		}
		else
		{
			final List<InventorySetupsItem> container = getContainerFromSlot(slot);
			container.get(slot.getIndexInSlot()).toggleIsFuzzy();
		}

		updateConfig(true, false);
		panel.refreshCurrentSetup();
	}

	public void setStackCompareOnSlot(final InventorySetupsSlot slot, final InventorySetupsStackCompareID newStackCompare)
	{
		if (panel.getCurrentSelectedSetup() == null)
		{
			return;
		}

		final List<InventorySetupsItem> container = getContainerFromSlot(slot);
		container.get(slot.getIndexInSlot()).setStackCompare(newStackCompare);

		updateConfig(true, false);
		panel.refreshCurrentSetup();
	}

	private void removeAdditionalFilteredItem(final InventorySetupsSlot slot, final Map<Integer, InventorySetupsItem> additionalFilteredItems)
	{

		assert panel.getCurrentSelectedSetup() != null : "Current setup is null";

		final int slotID = slot.getIndexInSlot();

		// Empty slot was selected to be removed, don't do anything
		if (slotID >= additionalFilteredItems.size())
		{
			return;
		}

		int j = 0;
		Integer keyToDelete = null;
		for (final Integer key : additionalFilteredItems.keySet())
		{
			if (slotID == j)
			{
				keyToDelete = key;
				break;
			}
			j++;
		}

		additionalFilteredItems.remove(keyToDelete);

	}

	public void updateSpellbookInSetup(int newSpellbook)
	{
		assert panel.getCurrentSelectedSetup() != null : "Setup is null";
		assert newSpellbook >= 0 && newSpellbook < 5 : "New spellbook out of range";

		clientThread.invokeLater(() ->
		{
			panel.getCurrentSelectedSetup().updateSpellbook(newSpellbook);
			updateConfig(true, false);
			panel.refreshCurrentSetup();
		});

	}

	public void updateNotesInSetup(final InventorySetup setup, final String text)
	{
		clientThread.invokeLater(() ->
		{
			setup.updateNotes(text);
			updateConfig(true, false);
		});
	}

	public void removeInventorySetup(final InventorySetup setup)
	{
		if (isDeletionConfirmed("Are you sure you want to permanently delete this inventory setup?", "Warning"))
		{
			// Remove the setup from any sections which have it
			for (final InventorySetupsSection section : sections)
			{
				section.getSetups().remove(setup.getName());
			}

			inventorySetupNames.remove(setup.getName());
			inventorySetups.remove(setup);
			panel.redrawOverviewPanel(false);
			updateConfig(true, true);
		}
	}

	public void removeSection(final InventorySetupsSection section)
	{
		if (isDeletionConfirmed("Are you sure you want to permanently delete this section?", "Warning"))
		{
			sectionNames.remove(section.getName());
			sections.remove(section);
			panel.redrawOverviewPanel(false);
			updateConfig(false, true);
		}
	}

	public void removeInventorySetupFromSection(final InventorySetup setup, final InventorySetupsSection section)
	{
		// No confirmation needed
		section.getSetups().remove(setup.getName());
		panel.redrawOverviewPanel(false);
		updateConfig(false, true);
	}

	private boolean isDeletionConfirmed(final String message, final String title)
	{
		int confirm = JOptionPane.showConfirmDialog(panel,
				message, title, JOptionPane.OK_CANCEL_OPTION);

		return confirm == JOptionPane.YES_OPTION;
	}

	public void updateConfig(boolean updateSetups, boolean updateSections)
	{
		if (updateSetups)
		{
			// update setups
			final String jsonSetups = gson.toJson(inventorySetups);
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_SETUPS, jsonSetups);
		}

		if (updateSections)
		{
			// update setups
			final String jsonSections = gson.toJson(sections);
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_SECTIONS, jsonSections);
		}

	}

	@Subscribe
	public void onSessionOpen(SessionOpen event)
	{
		final AccountSession session = sessionManager.getAccountSession();
		if (session != null && session.getUsername() != null)
		{
			// config will have changed to new account, load it up
			clientThread.invokeLater(() ->
			{
				loadConfig();
				return true;
			});
		}
	}

	@Subscribe
	public void onSessionClose(SessionClose event)
	{
		// config will have changed to local file
		clientThread.invokeLater(() ->
		{
			loadConfig();
			return true;
		});
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{

		// check to see that the container is the equipment or inventory
		ItemContainer container = event.getItemContainer();

		if (container == client.getItemContainer(InventoryID.INVENTORY))
		{
			panel.highlightInventory();
		}
		else if (container == client.getItemContainer(InventoryID.EQUIPMENT))
		{
			panel.highlightEquipment();
		}

	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		panel.highlightInventory();
		panel.highlightEquipment();
		panel.highlightSpellbook();
	}

	// Must be called on client thread!
	public int getCurrentSpellbook()
	{
		assert client.isClientThread() : "getCurrentSpellbook must be called on Client Thread";
		return client.getVarbitValue(SPELLBOOK_VARBIT);
	}

	public List<InventorySetupsItem> getNormalizedContainer(final InventorySetupsSlotID id)
	{
		switch (id)
		{
			case INVENTORY:
				return getNormalizedContainer(InventoryID.INVENTORY);
			case EQUIPMENT:
				return getNormalizedContainer(InventoryID.EQUIPMENT);
			case RUNE_POUCH:
				return getRunePouchData();
			case BOLT_POUCH:
				return getBoltPouchData();
			default:
				assert false : "Wrong slot ID!";
				return null;
		}
	}

	public List<InventorySetupsItem> getNormalizedContainer(final InventoryID id)
	{
		assert id == InventoryID.INVENTORY || id == InventoryID.EQUIPMENT : "invalid inventory ID";

		final ItemContainer container = client.getItemContainer(id);

		List<InventorySetupsItem> newContainer = new ArrayList<>();

		Item[] items = null;
		if (container != null)
		{
			items = container.getItems();
		}

		int size = id == InventoryID.INVENTORY ? NUM_INVENTORY_ITEMS : NUM_EQUIPMENT_ITEMS;

		for (int i = 0; i < size; i++)
		{

			final InventorySetupsStackCompareID stackCompareType = panel != null && panel.isStackCompareForSlotAllowed(InventorySetupsSlotID.fromInventoryID(id), i) ?
				config.stackCompareType() : InventorySetupsStackCompareID.None;
			if (items == null || i >= items.length)
			{
				// add a "dummy" item to fill the normalized container to the right size
				// this will be useful to compare when no item is in a slot
				newContainer.add(new InventorySetupsItem(-1, "", 0, config.fuzzy(), stackCompareType));
			}
			else
			{
				final Item item = items[i];
				String itemName = "";

				// only the client thread can retrieve the name. Therefore, do not use names to compare!
				if (client.isClientThread())
				{
					itemName = itemManager.getItemComposition(item.getId()).getName();
				}
				newContainer.add(new InventorySetupsItem(item.getId(), itemName, item.getQuantity(), config.fuzzy(), stackCompareType));
			}
		}

		return newContainer;
	}

	public void exportSetup(final InventorySetup setup)
	{
		final String json = gson.toJson(setup);
		final StringSelection contents = new StringSelection(json);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, null);

		JOptionPane.showMessageDialog(panel,
			"Setup data was copied to clipboard.",
			"Export Setup Succeeded",
			JOptionPane.PLAIN_MESSAGE);
	}

	public void exportSection(final InventorySetupsSection section)
	{
		final String json = gson.toJson(section);
		final StringSelection contents = new StringSelection(json);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, null);

		JOptionPane.showMessageDialog(panel,
				"Section data was copied to clipboard.",
				"Export Setup Succeeded",
				JOptionPane.PLAIN_MESSAGE);
	}

	public <T> void massExport(List<T> data, final String type, final String file_prefix)
	{

		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle("Choose Directory to Export " + type);
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

		int returnValue = fileChooser.showSaveDialog(panel);
		if (returnValue == JFileChooser.APPROVE_OPTION)
		{
			final File directory = fileChooser.getSelectedFile();
			String login_name = client.getLocalPlayer() != null ? "_" + client.getLocalPlayer().getName() : "";
			login_name = login_name.replace(" ", "_");
			String newFileName = directory.getAbsolutePath() + "/" + file_prefix + login_name + ".json";
			newFileName = newFileName.replace("\\", "/");
			try
			{
				final String json = gson.toJson(data);
				FileOutputStream outputStream = new FileOutputStream(newFileName);
				outputStream.write(json.getBytes());
				outputStream.close();
			}
			catch (Exception e)
			{
				log.error("Couldn't mass export " + type, e);
				JOptionPane.showMessageDialog(panel,
						"Failed to export " + type + ".",
						"Mass Export Failed",
						JOptionPane.PLAIN_MESSAGE);
				return;
			}

			JLabel messageLabel = new JLabel("<html><center>All " + type + " were exported successfully to<br>" + newFileName);
			messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
			JOptionPane.showMessageDialog(panel,
					messageLabel,
					"Mass Export Succeeded",
					JOptionPane.PLAIN_MESSAGE);
		}
	}

	public void importSetup()
	{
		try
		{
			final String setup = JOptionPane.showInputDialog(panel,
				"Enter setup data",
				"Import New Setup",
				JOptionPane.PLAIN_MESSAGE);

			// cancel button was clicked
			if (setup == null)
			{
				return;
			}

			Type type = new TypeToken<InventorySetup>()
			{

			}.getType();


			final InventorySetup newSetup = gson.fromJson(setup, type);

			if (isImportedSetupInvalid(newSetup))
			{
				throw new RuntimeException("Imported setup was missing required fields");
			}

			preProcessNewSetup(newSetup);
			inventorySetups.add(newSetup);
			inventorySetupNames.add(newSetup.getName());

			updateConfig(true, false);
			SwingUtilities.invokeLater(() -> panel.redrawOverviewPanel(false));

		}
		catch (Exception e)
		{
			log.error("Couldn't import setup", e);
			JOptionPane.showMessageDialog(panel,
				"Invalid setup data.",
				"Import Setup Failed",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	public void massImportSetups()
	{
		try
		{
			final Path path = showMassImportFolderDialog();
			if (path == null)
			{
				return;
			}
			final String json = new String(Files.readAllBytes(path));

			Type typeSetups = new TypeToken<ArrayList<InventorySetup>>()
			{

			}.getType();

			final ArrayList<InventorySetup> newSetups = gson.fromJson(json, typeSetups);

			// It's possible that the gson call succeeds but returns setups that have basically nothing
			// This can occur if trying to import a section file instead of a inventory setup file, since they share fields
			// Therefore, do some additional checking for required fields
			for (final InventorySetup setup : newSetups)
			{
				if (isImportedSetupInvalid(setup))
				{
					throw new RuntimeException("Mass import section file was missing required fields");
				}
			}

			for (final InventorySetup inventorySetup : newSetups)
			{
				preProcessNewSetup(inventorySetup);
				inventorySetups.add(inventorySetup);
				inventorySetupNames.add(inventorySetup.getName());
			}
			
			updateConfig(true, false);
			SwingUtilities.invokeLater(() -> panel.redrawOverviewPanel(false));

		}
		catch (Exception e)
		{
			log.error("Couldn't mass import setups", e);
			JOptionPane.showMessageDialog(panel,
					"Invalid setup data.",
					"Mass Import Setup Failed",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private boolean isImportedSetupInvalid(final InventorySetup setup)
	{
		return setup.getName() == null || setup.getInventory() == null || setup.getEquipment() == null || setup.getAdditionalFilteredItems() == null;
	}

	public void importSection()
	{
		try
		{
			final String section = JOptionPane.showInputDialog(panel,
					"Enter section data",
					"Import New Section",
					JOptionPane.PLAIN_MESSAGE);

			// cancel button was clicked
			if (section == null)
			{
				return;
			}

			Type type = new TypeToken<InventorySetupsSection>()
			{

			}.getType();

			final InventorySetupsSection newSection = gson.fromJson(section, type);

			if (isImportedSectionValid(newSection))
			{
				throw new RuntimeException("Imported section was missing required fields");
			}

			preProcessNewSection(newSection);
			sections.add(newSection);
			sectionNames.add(newSection.getName());
			updateConfig(false, true);
			SwingUtilities.invokeLater(() -> panel.redrawOverviewPanel(false));
		}
		catch (Exception e)
		{
			log.error("Couldn't import setup", e);
			JOptionPane.showMessageDialog(panel,
					"Invalid section data.",
					"Import section Failed",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void massImportSections()
	{
		try
		{
			final Path path = showMassImportFolderDialog();
			if (path == null)
			{
				return;
			}
			final String json = new String(Files.readAllBytes(path));

			Type typeSetups = new TypeToken<ArrayList<InventorySetupsSection>>()
			{

			}.getType();

			final ArrayList<InventorySetupsSection> newSections = gson.fromJson(json, typeSetups);

			// It's possible that the gson call succeeds but returns sections that have basically nothing
			// This can occur if trying to import an inventory setup file instead of a section file, since they share fields
			// Therefore, do some additional checking for required fields
			for (final InventorySetupsSection section : newSections)
			{
				if (isImportedSectionValid(section))
				{
					throw new RuntimeException("Mass import section file was missing required fields");
				}
			}

			for (final InventorySetupsSection section : newSections)
			{
				preProcessNewSection(section);
				sections.add(section);
				sectionNames.add(section.getName());
			}

			updateConfig(false, true);
			SwingUtilities.invokeLater(() -> panel.redrawOverviewPanel(false));

		}
		catch (Exception e)
		{
			log.error("Couldn't mass import sections", e);
			JOptionPane.showMessageDialog(panel,
					"Invalid section data.",
					"Mass Import Section Failed",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private boolean isImportedSectionValid(final InventorySetupsSection section)
	{
		return section.getName() == null || section.getSetups() == null;
	}

	private void preProcessNewSection(final InventorySetupsSection newSection)
	{
		final String newName = findNewName(newSection.getName(), sectionNames);
		newSection.setName(newName);

		// Remove any duplicates that came in when importing
		newSection.setSetups(newSection.getSetups().stream().distinct().collect(Collectors.toList()));

		// Remove setups which don't exist
		newSection.getSetups().removeIf(s -> !inventorySetupNames.contains(s));

	}

	private void preProcessNewSetup(final InventorySetup newSetup)
	{
		final String newName = findNewName(newSetup.getName(), inventorySetupNames);
		newSetup.setName(newName);
	}

	private Path showMassImportFolderDialog()
	{
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setDialogTitle("Choose Import File");
		FileFilter jsonFilter = new FileNameExtensionFilter("JSON files", "json");
		fileChooser.setFileFilter(jsonFilter);
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

		int returnValue = fileChooser.showOpenDialog(panel);

		if (returnValue == JFileChooser.APPROVE_OPTION)
		{
			return Paths.get(fileChooser.getSelectedFile().getAbsolutePath());
		}
		else
		{
			return null;
		}
	}

	@Override
	public void shutDown()
	{
		resetBankSearch(true);
		clientToolbar.removeNavigation(navButton);
	}

	public boolean isHighlightingAllowed()
	{
		return client.getGameState() == GameState.LOGGED_IN;
	}

	public boolean isFilteringAllowed()
	{
		boolean allowBasedOnActivePanel = navButton.isSelected() || !config.requireActivePanelFilter();

		return internalFilteringIsAllowed && allowBasedOnActivePanel;
	}

	private List<InventorySetupsItem> getContainerFromSlot(final InventorySetupsSlot slot)
	{
		assert slot.getParentSetup() == panel.getCurrentSelectedSetup() : "Setup Mismatch";

		switch (slot.getSlotID())
		{
			case INVENTORY:
				return slot.getParentSetup().getInventory();
			case EQUIPMENT:
				return slot.getParentSetup().getEquipment();
			case RUNE_POUCH:
				return slot.getParentSetup().getRune_pouch();
			case BOLT_POUCH:
				return slot.getParentSetup().getBoltPouch();
			default:
				assert false : "Invalid ID given";
				return null;
		}
	}

	private void loadConfig()
	{
		inventorySetupNames = new HashSet<>();
		sectionNames = new HashSet<>();

		Type setupType = new TypeToken<ArrayList<InventorySetup>>()
		{

		}.getType();
		Type sectionType = new TypeToken<ArrayList<InventorySetupsSection>>()
		{

		}.getType();

		inventorySetups = loadData(CONFIG_KEY_SETUPS, setupType);
		sections = loadData(CONFIG_KEY_SECTIONS, sectionType);
		// Fix names of setups from config if there are duplicate names
		for (final InventorySetupsSection section : sections)
		{
			final String newName = findNewName(section.getName(), sectionNames);
			section.setName(newName);
			sectionNames.add(newName);

			// Remove any duplicates that exist
			List<String> uniqueSetups = section.getSetups().stream().distinct().collect(Collectors.toList());
			section.setSetups(uniqueSetups);
		}

		clientThread.invokeLater(() ->
		{
			updateOldSetups();

			// Remove setups that don't exist in each section
			for (final InventorySetupsSection section : sections)
			{
				// Remove setups which don't exist in a section
				section.getSetups().removeIf(s -> !inventorySetupNames.contains(s));
			}

			SwingUtilities.invokeLater(() -> panel.redrawOverviewPanel(true));
		});
	}

	private <T> List<T> loadData(final String configKey, Type type)
	{
		final String storedData = configManager.getConfiguration(CONFIG_GROUP, configKey);
		if (Strings.isNullOrEmpty(storedData))
		{
			return new ArrayList<>();
		}
		else
		{
			try
			{
				// serialize the internal data structure from the json in the configuration
				return gson.fromJson(storedData, type);
			}
			catch (Exception e)
			{
				log.error("Exception occurred while loading data", e);
				return new ArrayList<>();
			}
		}
	}

	public boolean setupContainsItem(final InventorySetup setup, int itemID)
	{
		// So place holders will show up in the bank.
		itemID = itemManager.canonicalize(itemID);

		// Check if this item (inc. placeholder) is in the additional filtered items
		if (additionalFilteredItemsHasItem(itemID, setup.getAdditionalFilteredItems()))
		{
			return true;
		}

		// check the rune pouch to see if it has the item (runes in this case)
		if (setup.getRune_pouch() != null)
		{
			if (checkIfContainerContainsItem(itemID, setup.getRune_pouch()))
			{
				return true;
			}
		}

		// check the bolt pouch to see if it has the item (bolts in this case)
		if (setup.getBoltPouch() != null)
		{
			if (checkIfContainerContainsItem(itemID, setup.getBoltPouch()))
			{
				return true;
			}
		}

		return checkIfContainerContainsItem(itemID, setup.getInventory()) ||
			checkIfContainerContainsItem(itemID, setup.getEquipment());
	}

	private boolean checkIfContainerContainsItem(int itemID, final List<InventorySetupsItem> setupContainer)
	{
		// So place holders will show up in the bank.
		itemID = itemManager.canonicalize(itemID);

		for (final InventorySetupsItem item : setupContainer)
		{
			// For equipped weight reducing items or noted items in the inventory
			int setupItemId = itemManager.canonicalize(item.getId());
			if (getProcessedID(item.isFuzzy(), itemID) == getProcessedID(item.isFuzzy(), setupItemId))
			{
				return true;
			}
		}

		return false;
	}

	private int getProcessedID(boolean isFuzzy, int itemId)
	{
		// use fuzzy mapping if needed
		if (isFuzzy)
		{
			return InventorySetupsVariationMapping.map(itemId);
		}

		return itemId;
	}

	private boolean checkAndUpdateSlotIfRunePouchWasSelected(final InventorySetupsSlot slot, final InventorySetupsItem oldItem, final InventorySetupsItem newItem)
	{

		if (isItemRunePouch(newItem.getId()))
		{

			if (slot.getSlotID() != InventorySetupsSlotID.INVENTORY)
			{

				SwingUtilities.invokeLater(() ->
				{
					JOptionPane.showMessageDialog(panel,
						"You can't have a Rune Pouch there.",
						"Invalid Item",
						JOptionPane.ERROR_MESSAGE);
				});

				return false;
			}

			// only display this message if we aren't replacing a rune pouch with a new rune pouch
			if (slot.getParentSetup().getRune_pouch() != null && !isItemRunePouch(oldItem.getId()))
			{
				SwingUtilities.invokeLater(() ->
				{
					JOptionPane.showMessageDialog(panel,
						"You can't have two Rune Pouches.",
						"Invalid Item",
						JOptionPane.ERROR_MESSAGE);
				});
				return false;
			}

			slot.getParentSetup().updateRunePouch(getRunePouchData());
		}
		else if (isItemRunePouch(oldItem.getId()))
		{
			// if the old item is a rune pouch, need to update it to null 
			slot.getParentSetup().updateRunePouch(null);
		}

		return true;
	}

	private boolean checkAndUpdateSlotIfBoltPouchWasSelected(final InventorySetupsSlot slot, final InventorySetupsItem oldItem, final InventorySetupsItem newItem)
	{

		if (isItemBoltPouch(newItem.getId()))
		{

			if (slot.getSlotID() != InventorySetupsSlotID.INVENTORY)
			{

				SwingUtilities.invokeLater(() ->
				{
					JOptionPane.showMessageDialog(panel,
						"You can't have a Bolt Pouch there.",
						"Invalid Item",
						JOptionPane.ERROR_MESSAGE);
				});

				return false;
			}

			// only display this message if we aren't replacing a bolt pouch with a new bolt pouch
			if (slot.getParentSetup().getBoltPouch() != null && !isItemBoltPouch(oldItem.getId()))
			{
				SwingUtilities.invokeLater(() ->
				{
					JOptionPane.showMessageDialog(panel,
						"You can't have two Bolt Pouches.",
						"Invalid Item",
						JOptionPane.ERROR_MESSAGE);
				});
				return false;
			}

			slot.getParentSetup().updateBoltPouch(getBoltPouchData());
		}
		else if (isItemBoltPouch(oldItem.getId()))
		{
			// if the old item is a bolt pouch, need to update it to null
			slot.getParentSetup().updateBoltPouch(null);
		}

		return true;
	}

	private boolean isItemRunePouch(final int itemId)
	{
		return itemId == ItemID.RUNE_POUCH || itemId == ItemID.RUNE_POUCH_L;
	}

	private boolean isItemBoltPouch(final int itemId)
	{
		return itemId == ItemID.BOLT_POUCH;
	}

	private boolean containerContainsRunePouch(final List<InventorySetupsItem> container)
	{
		return checkIfContainerContainsItem(ItemID.RUNE_POUCH, container) ||
			checkIfContainerContainsItem(ItemID.RUNE_POUCH_L, container);
	}

	private boolean containerContainsBoltPouch(final List<InventorySetupsItem> container)
	{
		return checkIfContainerContainsItem(ItemID.BOLT_POUCH, container);
	}

	public void openColorPicker(String title, Color startingColor, Consumer<Color> onColorChange)
	{

		RuneliteColorPicker colorPicker = getColorPickerManager().create(
				SwingUtilities.windowForComponent(panel),
				startingColor,
				title,
				false);

		colorPicker.setLocation(panel.getLocationOnScreen());
		colorPicker.setOnColorChange(onColorChange);

		colorPicker.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				updateConfig(true, true);
			}
		});

		colorPicker.setVisible(true);
	}

	public int parseTextInputAmount(String input)
	{
		// only take the first 10 characters (max amount is 2.147B which is only 10 digits)
		if (input.length() > 10)
		{
			return Integer.MAX_VALUE;
		}
		input = input.toLowerCase();

		char finalChar = input.charAt(input.length() - 1);
		int factor = 1;
		if (Character.isLetter(finalChar))
		{
			input = input.substring(0, input.length() - 1);
			switch (finalChar)
			{
				case 'k':
					factor = 1000;
					break;
				case 'm':
					factor = 1000000;
					break;
				case 'b':
					factor = 1000000000;
					break;
			}
		}

		// limit to max int value
		long quantityLong = Long.parseLong(input) * factor;
		int quantity = (int) Math.min(quantityLong, Integer.MAX_VALUE);
		quantity = Math.max(quantity, 1);

		return quantity;
	}

	private void updateOldSetups()
	{
		for (final InventorySetup setup : inventorySetups)
		{
			if (setup.getRune_pouch() == null && containerContainsRunePouch(setup.getInventory()))
			{
				setup.updateRunePouch(getRunePouchData());
			}
			if (setup.getBoltPouch() == null && containerContainsBoltPouch(setup.getInventory()))
			{
				setup.updateBoltPouch(getBoltPouchData());
			}
			if (setup.getNotes() == null)
			{
				setup.updateNotes("");
			}
			if (setup.getAdditionalFilteredItems() == null)
			{
				setup.updateAdditionalItems(new HashMap<>());
			}

			final String newName = findNewName(setup.getName(), inventorySetupNames);
			setup.setName(newName);
			inventorySetupNames.add(newName);
		}

	}

	public void updateSetupName(final InventorySetup setup, String newName)
	{
		final String originalName = setup.getName();
		inventorySetupNames.remove(originalName);
		inventorySetupNames.add(newName);
		setup.setName(newName);

		for (final InventorySetupsSection section : sections)
		{
			final List<String> names = section.getSetups();
			for (int i = 0; i < names.size(); i++)
			{
				if (names.get(i).equals(originalName))
				{
					names.set(i, newName);
					// There should be no duplicates in a section so break here
					break;
				}
			}
		}

		// config will already be updated by caller so need to update it here
	}

	public void updateSectionName(final InventorySetupsSection section, String newName)
	{
		section.setName(newName);

		// config will already be updated by caller so need to update it here
	}

	private String findNewName(String originalName, final Set<String> objects)
	{
		// Do not allow names of more than MAX_SETUP_NAME_LENGTH chars
		if (originalName.length() > MAX_SETUP_NAME_LENGTH)
		{
			originalName = originalName.substring(0, MAX_SETUP_NAME_LENGTH);
		}

		// Fix duplicate name by adding an incrementing number to the duplicate
		String newName = originalName;
		int i = 1;
		while (objects.contains(newName) || newName.isEmpty())
		{
			String i_str = String.valueOf(i);
			if (originalName.length() + i_str.length() > MAX_SETUP_NAME_LENGTH)
			{
				int chars_to_cut_off = i_str.length() - (MAX_SETUP_NAME_LENGTH - originalName.length());
				newName = originalName.substring(0, MAX_SETUP_NAME_LENGTH - chars_to_cut_off) + i++;
			}
			else
			{
				newName = originalName + i++;
			}
		}
		return newName;
	}

	private String fixOldJSONData(final String json)
	{
		final Gson gson = new Gson();
		JsonElement je = gson.fromJson(json, JsonElement.class);
		JsonArray ja = je.getAsJsonArray();
		for (JsonElement elem : ja)
		{
			JsonObject setup = elem.getAsJsonObject();

			// Example if needed in the future
//			if (setup.getAsJsonPrimitive("stackDifference").isBoolean())
//			{
//				int stackDiff = setup.get("stackDifference").getAsBoolean() ? 1 : 0;
//				setup.remove("stackDifference");
//				setup.addProperty("stackDifference", stackDiff);
//			}
		}
		return je.toString();
	}

}
