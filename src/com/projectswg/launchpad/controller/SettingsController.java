/*
 * 
 * This file is part of ProjectSWG Launchpad.
 *
 * ProjectSWG Launchpad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * ProjectSWG Launchpad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with ProjectSWG Launchpad.  If not, see <http://www.gnu.org/licenses/>.      
 *
 */

package com.projectswg.launchpad.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import com.projectswg.launchpad.ProjectSWG;
import com.projectswg.launchpad.service.Manager;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class SettingsController implements ModalComponent
{
	@FXML
	private Accordion settingsRoot;
	
	@FXML
	private TitledPane winePane;
	
	@FXML
	private Slider animationSlider;
	
	@FXML
	private Button settingsSwgFolderButton, settingsPswgFolderButton, pingButton, deleteGameProfilesButton;
	
	@FXML
	private Button refreshThemesButton, removeLoginServerButton, addLoginServerButton, wineBinaryButton;
	
	@FXML
	private Button binaryButton, gameFeaturesButton;
	
	@FXML
	private TextField wineArgumentsTextField, wineEnvironmentVariablesTextField;
	
	@FXML
	private CheckBox closeAfterLaunchCheckBox, captureCheckBox, localhostCheckBox, debugCheckBox;
	
	@FXML
	private CheckBox loginServerLockedCheckBox, openOnLaunchCheckBox, soundCheckBox;
	
	@FXML
	private ComboBox<String> loginServerComboBox, themeComboBox;
	
	@FXML
	private Pane pingDisplayPane, swgFolderDisplayPane, pswgFolderDisplayPane;
	
	@FXML
	private TextField hostnameTextField, portTextField, statusPortTextField;
	
	@FXML
	private Hyperlink pswgHyperlink, showHyperlink, resetBinaryHyperlink, resetGameFeaturesHyperlink;
	
	@FXML
	private Text licenseText;

	private static final String LABEL = "Settings";
	private static final int WINE_PANE_INDEX = 3;
	
	private MainController mainController;
	private ModalController modalController;
	private NodeDisplay pingDisplay;
	private NodeDisplay swgFolderDisplay, pswgFolderDisplay;
	private Manager manager;
	private ProgressIndicator progressIndicator;
	private EventHandler<MouseEvent> buttonHover;
	private EventHandler<MouseEvent> buttonPress;
	private EventHandler<MouseEvent> comboClicked;
	
	public SettingsController()
	{
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1)
	{
		// keep one pane open at all times
		settingsRoot.expandedPaneProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue != null)
				oldValue.setCollapsible(true);
			if (newValue != null)
				Platform.runLater(() -> {
					newValue.setCollapsible(false);
					ProjectSWG.playSound("pane_expand");
				});
		});
		
		settingsRoot.setExpandedPane(settingsRoot.getPanes().get(0));
		
		progressIndicator = new ProgressIndicator();
		progressIndicator.setMaxSize(15, 15);
		progressIndicator.setPrefSize(15, 15);
		progressIndicator.setMinSize(15, 15);
	}
	
	@Override
	public void init(MainController mainController)
	{
		this.mainController = mainController;
		this.manager = mainController.getManager();
		
		buttonHover = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				ProjectSWG.playSound("button_hover");
			}
		};
		
		buttonPress = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				ProjectSWG.playSound("button_press");
			}
		};
		
		comboClicked = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				ProjectSWG.playSound("combo_clicked");
			}
		};
		
		pingDisplay = new NodeDisplay(pingDisplayPane);
		swgFolderDisplay = new NodeDisplay(swgFolderDisplayPane);
		pswgFolderDisplay = new NodeDisplay(pswgFolderDisplayPane);
		
		initGeneralPane();
		initSetupPane();
		initDeveloperPane();
		if (ProjectSWG.isWindows())
			settingsRoot.getPanes().remove(WINE_PANE_INDEX);
		else
			initWinePane();
		initAboutPane();
		
		animationSlider.setValue(ProjectSWG.PREFS.getInt("animation", ProjectSWG.ANIMATION_NONE));
	}
	
	@Override
	public String getLabel()
	{
		return LABEL;
	}
	
	public void initGeneralPane()
	{
		// sound
		soundCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.putBoolean("sound", newValue);
		});
		soundCheckBox.setSelected(ProjectSWG.PREFS.getBoolean("sound", false));
		
		// close on launch
		closeAfterLaunchCheckBox.setSelected(ProjectSWG.PREFS.getBoolean("close_after_launch", false));
		closeAfterLaunchCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.putBoolean("close_after_launch", newValue);
		});
		
		// theme
		refreshThemesButton.setOnMouseEntered(buttonHover);
		refreshThemesButton.setOnMouseClicked(buttonPress);
		refreshThemesButton.setOnAction((e) -> {
			refreshThemeList();
		});
		refreshThemeList();
		
		themeComboBox.setOnMouseClicked(comboClicked);
		themeComboBox.setValue(ProjectSWG.PREFS.get("theme", "Default"));
		themeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue == null || newValue == null)
				return;
			mainController.removeInstanceListener();
			((LogController)mainController.getPswg().getControllers().get("log")).removeDebugListener();
			mainController.getPswg().loadTheme(newValue);
			Platform.runLater(() -> {
				ModalController modal = (ModalController)mainController.getPswg().getControllers().get("modal");
				modal.loadComponent((SettingsController)mainController.getPswg().getControllers().get("settings"));
			});
		});
		
		// animation
		animationSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (!animationSlider.valueChangingProperty().getValue())
				ProjectSWG.playSound("slider_select");
				ProjectSWG.PREFS.putInt("animation", newValue.intValue());
		});
	}
	
	public void initSetupPane()
	{
		initSetupLoginServerSettings();
		initSetupFolderSettings();
	}
	
	public void refreshLoginServerComboBox()
	{
		loginServerLockedCheckBox.setSelected(true);
		
		Preferences loginServersNode = ProjectSWG.PREFS.node("login_servers");
		loginServerComboBox.getItems().clear();
		try {
			for (String server : loginServersNode.keys())
				loginServerComboBox.getItems().add(server);
		} catch (BackingStoreException e1) {
			ProjectSWG.log("Refresh Login Servers Error: " + e1.toString());
		}
		
		loginServerComboBox.setValue(ProjectSWG.PREFS.get("login_server", Manager.PSWG_LOGIN_SERVER_NAME));
	}
	
	public void initSetupLoginServerSettings()
	{
		// fix
		String hostnamePattern = "[0-9a-zA-Z\\.-]*";
		String portPattern = "^[0-9]{0,6}$";
		
		ChangeListener<String> hostnameTextChangeListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Pattern.matches(hostnamePattern, newValue))
					manager.setLoginServerHostname(loginServerComboBox.getValue(), newValue);
				else
					hostnameTextField.setText(oldValue);
			}
		};
		
		ChangeListener<String> portTextChangeListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Pattern.matches(portPattern, newValue))
					manager.setLoginServerPort(loginServerComboBox.getValue(), newValue);
				else
					portTextField.setText(oldValue);
			}
		};
		
		ChangeListener<String> statusPortTextChangeListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Pattern.matches(portPattern, newValue))
					manager.setLoginServerStatusPort(loginServerComboBox.getValue(), newValue);
				else
					statusPortTextField.setText(oldValue);
			}
		};
		
		loginServerLockedCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				hostnameTextField.setDisable(true);
				hostnameTextField.textProperty().removeListener(hostnameTextChangeListener);
				hostnameTextField.textProperty().bind(manager.getLoginServerHost());

				portTextField.setDisable(true);
				portTextField.textProperty().removeListener(portTextChangeListener);
				portTextField.textProperty().bind(manager.getLoginServerPlayPort());

				statusPortTextField.setDisable(true);
				statusPortTextField.textProperty().removeListener(statusPortTextChangeListener);
				statusPortTextField.textProperty().bind(manager.getLoginServerPingPort());
			} else {
				hostnameTextField.setDisable(false);
				hostnameTextField.textProperty().unbind();
				hostnameTextField.textProperty().addListener(hostnameTextChangeListener);

				portTextField.setDisable(false);
				portTextField.textProperty().unbind();
				portTextField.textProperty().addListener(portTextChangeListener);

				statusPortTextField.setDisable(false);
				statusPortTextField.textProperty().unbind();
				statusPortTextField.textProperty().addListener(statusPortTextChangeListener);
			}
		});
		
		loginServerComboBox.setOnMouseClicked(comboClicked);
		loginServerComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null)
				return;
			
			ProjectSWG.log("Setting server: " + newValue);
			mainController.getPlayButtonTooltip().setText("Profile: " + newValue);
			
			loginServerLockedCheckBox.setSelected(true);
			if (newValue.equals(Manager.PSWG_LOGIN_SERVER_NAME)) {
				loginServerLockedCheckBox.setDisable(true);
				removeLoginServerButton.setDisable(true);
			} else if (oldValue != null && oldValue.equals(Manager.PSWG_LOGIN_SERVER_NAME)) {
				loginServerLockedCheckBox.setDisable(false);
				removeLoginServerButton.setDisable(false);
			}
			
			manager.setLoginServerByName(newValue);
		});
		refreshLoginServerComboBox();
		
		pingButton.setOnMouseEntered(buttonHover);
		pingButton.setOnMouseClicked(buttonPress);
		pingButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_pressed");
			manager.pingLoginServer();
		});
		
		manager.getPingService().setOnSucceeded((e) -> {
			String result = (String)e.getSource().getValue();
			ProjectSWG.log("pingerOut: " + result);
			pingDisplay.queueString("" + result);
		});

		manager.getPingService().setOnRunning((e) -> {
			pingDisplay.queueNode(progressIndicator);
		});
		
		addLoginServerButton.setOnMouseEntered(buttonHover);
		addLoginServerButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle("Add Server");
			dialog.setHeaderText("Please enter a name for the server.");
			dialog.setContentText("Server name: ");
			Optional<String> result = dialog.showAndWait();
			result.ifPresent(name -> {
				manager.addLoginServer(name);
				refreshLoginServerComboBox();
				loginServerComboBox.setValue(name);
				loginServerLockedCheckBox.setSelected(false);
			});
		});
		
		removeLoginServerButton.setOnMouseEntered(buttonHover);
		removeLoginServerButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Remove Server");
			alert.setHeaderText("Are you sure you want to remove this server?");
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				String val = loginServerComboBox.getValue();
				loginServerComboBox.getSelectionModel().select(0);
				manager.removeLoginServer(val);
				refreshLoginServerComboBox();
			}
		});
	}
	
	public void initSetupFolderSettings()
	{
		final Tooltip settingsSwgFolderTooltip = new Tooltip();
		final Tooltip settingsPswgFolderTooltip = new Tooltip();
		
		settingsSwgFolderButton.textProperty().bind(manager.getSwgFolder());
		settingsSwgFolderButton.setOnMouseEntered(buttonHover);

		settingsSwgFolderTooltip.textProperty().bind(manager.getSwgFolder());
		settingsSwgFolderButton.setTooltip(settingsSwgFolderTooltip);
		settingsPswgFolderButton.textProperty().bind(manager.getPswgFolder());

		settingsPswgFolderTooltip.textProperty().bind(manager.getPswgFolder());
		settingsPswgFolderButton.setTooltip(settingsPswgFolderTooltip);
		settingsPswgFolderButton.setOnMouseEntered(buttonHover);
		
		manager.getState().addListener((observable, oldValue, newValue) -> {
			switch (newValue.intValue()) {
			case Manager.STATE_INIT:
				swgFolderDisplay.queueString(ProjectSWG.XMARK);
				pswgFolderDisplay.queueString(ProjectSWG.XMARK);
				settingsPswgFolderButton.setDisable(true);
				break;
			
			case Manager.STATE_SWG_SETUP_REQUIRED:
				swgFolderDisplay.queueString(ProjectSWG.XMARK);
				if (manager.getPswgFolder().getValue().equals(""))
					pswgFolderDisplay.queueString(ProjectSWG.XMARK);
				else
					pswgFolderDisplay.queueString(ProjectSWG.CHECKMARK);
				settingsPswgFolderButton.setDisable(true);
				break;
				
			case Manager.STATE_SWG_SCANNING:
				swgFolderDisplay.queueNode(progressIndicator);
				break;
				
			case Manager.STATE_PSWG_SCAN_REQUIRED:
				swgFolderDisplay.queueString(ProjectSWG.CHECKMARK);
				pswgFolderDisplay.queueString(ProjectSWG.XMARK);
				settingsPswgFolderButton.setDisable(false);
				break;
				
			case Manager.STATE_PSWG_SCANNING:
				pswgFolderDisplay.queueNode(progressIndicator);
				break;
			
			default:
				swgFolderDisplay.queueString(ProjectSWG.CHECKMARK);
				pswgFolderDisplay.queueString(ProjectSWG.CHECKMARK);
				settingsPswgFolderButton.setDisable(false);
			}
		});
		
		settingsSwgFolderButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select SWG folder");
			File file = directoryChooser.showDialog(mainController.getStage());
			if (file == null || !file.isDirectory())
				return;
			
			String swgPath =  file.getAbsolutePath();
			manager.getSwgFolder().set(swgPath);
		});
		
		settingsPswgFolderButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select ProjectSWG folder");
			File file = directoryChooser.showDialog(mainController.getStage());
			if (file == null || !file.isDirectory())
				return;

			String pswgPath = file.getAbsolutePath();
			manager.getPswgFolder().set(pswgPath);
		});
	}
	
	public void initDeveloperPane()
	{
		localhostCheckBox.setSelected(ProjectSWG.PREFS.getBoolean("localhost", false));
		localhostCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.putBoolean("localhost", newValue);
		});
		
		captureCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.putBoolean("capture", newValue);
			openOnLaunchCheckBox.setDisable(!newValue);
		});
		captureCheckBox.setSelected(ProjectSWG.PREFS.getBoolean("capture", false));
		
		openOnLaunchCheckBox.setSelected(ProjectSWG.PREFS.getBoolean("open_on_launch", false));
		openOnLaunchCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.putBoolean("open_on_launch", newValue);
		});
		
		manager.getPswgFolder().addListener((observable, oldValue, newValue) -> {
			if (newValue == null)
				return;
			if (newValue.equals(""))
				deleteGameProfilesButton.setDisable(true);
			else
				deleteGameProfilesButton.setDisable(false);
		});
		
		if (ProjectSWG.PREFS.getBoolean("debug", false))
			debugCheckBox.setSelected(true);
		else
			showHyperlink.setDisable(true);
		
		debugCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.putBoolean("debug", newValue);
			showHyperlink.setDisable(!newValue);
			((LogController)mainController.getPswg().getControllers().get("log")).removeDebugListener();
			if (newValue)
				((LogController)mainController.getPswg().getControllers().get("log")).addDebugListener();
		});
		
		showHyperlink.setOnAction((e) -> {
			((LogController)mainController.getPswg().getControllers().get("log")).show();
		});
		
		final Tooltip binaryTooltip = new Tooltip();
		binaryTooltip.textProperty().bind(manager.getBinary());
		binaryButton.textProperty().bind(manager.getBinary());
		binaryButton.setTooltip(binaryTooltip);
		binaryButton.setOnMouseEntered(buttonHover);
		binaryButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Set Binary Location");
			File file = fileChooser.showOpenDialog(mainController.getStage());
			if (file == null || !file.isFile())
				return;
			
			String bin =  file.getAbsolutePath();
			manager.getBinary().set(bin);
		});
		
		resetBinaryHyperlink.setOnAction((e) -> {
			manager.getBinary().set(manager.getPswgFolder().getValue() + "/SwgClient_r.exe");
		});
		
		final Tooltip gameFeaturesTooltip = new Tooltip();
		gameFeaturesTooltip.textProperty().bind(manager.getGameFeatures());
		gameFeaturesButton.textProperty().bind(manager.getGameFeatures());
		gameFeaturesButton.setTooltip(binaryTooltip);
		gameFeaturesButton.setOnMouseEntered(buttonHover);
		gameFeaturesButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle("Game Features");
			dialog.setHeaderText("Change");
			dialog.setContentText("Game features: ");
			Optional<String> result = dialog.showAndWait();
			result.ifPresent(gf -> {
				manager.getGameFeatures().set(gf);
			});
		});
		
		resetGameFeaturesHyperlink.setOnAction((e) -> {
			manager.getGameFeatures().set(Manager.GAME_FEATURES);
		});
		
		manager.getState().addListener((observable, oldValue, newValue) -> {
			switch (newValue.intValue()) {
			case Manager.STATE_SWG_SETUP_REQUIRED:
			case Manager.STATE_SWG_SCANNING:
			case Manager.STATE_PSWG_SETUP_REQUIRED:
			case Manager.STATE_PSWG_SCAN_REQUIRED:
			case Manager.STATE_PSWG_SCANNING:
			case Manager.STATE_UPDATE_REQUIRED:
			case Manager.STATE_UPDATING:
				binaryButton.setDisable(true);
				resetBinaryHyperlink.setDisable(true);
				gameFeaturesButton.setDisable(true);
				resetGameFeaturesHyperlink.setDisable(true);
				break;
			
			case Manager.STATE_WINE_REQUIRED:
			case Manager.STATE_PSWG_READY:
				binaryButton.setDisable(false);
				resetBinaryHyperlink.setDisable(false);
				gameFeaturesButton.setDisable(false);
				resetGameFeaturesHyperlink.setDisable(false);
				break;
				
			default:
			}
		});
		
		deleteGameProfilesButton.setOnMouseEntered(buttonHover);
		deleteGameProfilesButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			
			String pswgFolder = manager.getPswgFolder().getValue();
			if (pswgFolder.equals(""))
				return;
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Delete game profiles");
			alert.setHeaderText(String.format("This will remove the %s/profiles' folder.", pswgFolder));
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK)
				try {
					Manager.removeRecursive(Paths.get(pswgFolder + "/profiles"));
				} catch (IOException e1) {
					e1.printStackTrace();
					alert = new Alert(AlertType.ERROR);
					alert.setTitle("Error");
					alert.setHeaderText("An error occurred.");
					alert.showAndWait();
				}
		});
	}
	
	public void initWinePane()
	{
		wineBinaryButton.textProperty().bind(manager.getWineBinary());
		final Tooltip wineBinaryTooltip = new Tooltip();
		wineBinaryTooltip.textProperty().bind(manager.getWineBinary());
		wineBinaryButton.setTooltip(wineBinaryTooltip);
		wineBinaryButton.setOnMouseEntered(buttonHover);
		wineBinaryButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Set Wine Location");
			File file = fileChooser.showOpenDialog(mainController.getStage());
			if (file == null || !file.isFile())
				return;
			
			String wineBin =  file.getAbsolutePath();
			manager.getWineBinary().set(wineBin);
		});
		
		wineArgumentsTextField.setText(ProjectSWG.PREFS.get("wine_arguments", ""));
		wineArgumentsTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			manager.getWineArguments().set(newValue);
		});
		
		wineEnvironmentVariablesTextField.setText(ProjectSWG.PREFS.get("wine_environment_variables", ""));
		wineEnvironmentVariablesTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			manager.getWineEnvironmentVariables().set(newValue);
		});
	}
	
	public void initAboutPane()
	{
		/*
		pswgHyperlink.setText("www.projectswg.com");
		pswgHyperlink.setOnAction((e) -> {
			try {
				java.awt.Desktop.getDesktop().browse(new URI(ProjectSWG.PSWG_URL));
			} catch (Exception e1) {
				ProjectSWG.log(e1.toString());
			}
		});
	 	*/
		
		licenseText.setText(
			"\n\nThis file is part of ProjectSWG Launchpad.\n" +
			"ProjectSWG Launchpad is free software: you can redistribute " + 
          	"it and/or modify it under the terms of the GNU Affero General Public License " +
          	"as published by the Free Software Foundation, either version 3 of " +
          	"the License, or (at your option) any later version. ProjectSWG Launchpad is distributed " +
          	"in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even " +
          	"the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. " +
          	"See the GNU Affero General Public License for more details. You should have received a " +
          	"copy of the GNU Affero General Public License along with ProjectSWG Launchpad. " +
          	"If not, see \"http://www.gnu.org/licenses/.\""
        );
	}
	
	public void refreshThemeList()
	{
		File file = new File(ProjectSWG.THEMES_FOLDER);
		if (!file.isDirectory()) {
			ProjectSWG.log("Folder not found: " + ProjectSWG.THEMES_FOLDER);
			return;
		}
		
		String[] subdirs = file.list((current, name) -> {
			return new File(current, name).isDirectory();
		});

		ArrayList<String> themeList = new ArrayList<>();
		themeList.add("Default");

		for (String dir : subdirs) {
			ProjectSWG.log("folder: " + ProjectSWG.THEMES_FOLDER + "/" + dir + "/" + ProjectSWG.CSS_NAME);
			file = new File(ProjectSWG.THEMES_FOLDER + "/" + dir + "/" + ProjectSWG.CSS_NAME);
			if (file.isFile())
				themeList.add(dir);
		}
		
		String currentSelection = themeComboBox.getValue();
		themeComboBox.getItems().clear();
		themeComboBox.getItems().addAll(themeList);
		if (currentSelection == null)
			themeComboBox.getSelectionModel().select(0);
		else
			themeComboBox.setValue(currentSelection);
		ProjectSWG.log("Theme list refreshed");
	}
	
	@Override
	public Parent getRoot()
	{
		return settingsRoot;
	}
}
