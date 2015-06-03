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

import com.projectswg.launchpad.PSWG;
import com.projectswg.launchpad.service.Manager;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
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
	private TextField wineArgumentsTextField, wineEnvironmentVariablesTextField;
	
	@FXML
	private CheckBox closeAfterLaunchCheckBox, debugCheckBox, localhostCheckBox;
	
	@FXML
	private CheckBox loginServerLockedCheckBox, openOnLaunchButton;
	
	@FXML
	private ComboBox<String> loginServerComboBox, themeComboBox;
	
	@FXML
	private Pane pingDisplayPane, themeDisplayPane, swgFolderDisplayPane, pswgFolderDisplayPane;
	
	@FXML
	private TextField hostnameTextField, portTextField, statusPortTextField;
	
	@FXML
	private Tooltip settingsSwgFolderTooltip, settingsPswgFolderTooltip;
	
	@FXML
	private Hyperlink pswgHyperlink;
	
	@FXML
	private Text licenseText;
	
	
	private static final String LABEL = "Settings";
	private static final int WINE_PANE_INDEX = 3;
	
	private MainController mainController;
	private ModalController modalController;
	private NodeDisplay pingDisplay, themeDisplay;
	private NodeDisplay swgFolderDisplay, pswgFolderDisplay;
	
	
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
				});
		});
		
		settingsRoot.setExpandedPane(settingsRoot.getPanes().get(0));
	}
	
	@Override
	public void init(ModalController modalController)
	{
		this.modalController = modalController;
		this.mainController = modalController.getMain();
		
		themeDisplay = new NodeDisplay(themeDisplayPane);
		pingDisplay = new NodeDisplay(pingDisplayPane);
		swgFolderDisplay = new NodeDisplay(swgFolderDisplayPane);
		pswgFolderDisplay = new NodeDisplay(pswgFolderDisplayPane);
		
		initGeneralPane();
		initSetupPane();
		initDeveloperPane();
		initWinePane();
		initInfoPane();
	}
	
	@Override
	public void onShow()
	{
		animationSlider.setValue(PSWG.PREFS.getInt("animation", 2));
		if (mainController.getManager().getSwgFolder().getValue().equals(""))
			swgFolderDisplay.queueString(MainController.XMARK);
		else
			swgFolderDisplay.queueString(MainController.CHECKMARK);
		
		if (mainController.getManager().getSwgFolder().getValue().equals(""))
			pswgFolderDisplay.queueString(MainController.XMARK);
		else
			pswgFolderDisplay.queueString(MainController.CHECKMARK);
	}
	
	@Override
	public String getLabel()
	{
		return LABEL;
	}
	
	public void initGeneralPane()
	{
		// animation preference
		animationSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (!animationSlider.valueChangingProperty().getValue())
				mainController.getAnimationLevel().setValue(newValue);;
		});
		
		closeAfterLaunchCheckBox.setSelected(PSWG.PREFS.getBoolean("close_after_launch", false));
		closeAfterLaunchCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.putBoolean("close_after_launch", newValue);
		});
		
		// Themes
		refreshThemesButton.setOnAction((e) -> {
			refreshThemeList();
			themeDisplay.queueString("Theme list refreshed.");
		});
		refreshThemeList();

		themeComboBox.setValue(PSWG.PREFS.get("theme", ""));
		themeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) {
				PSWG.log("themeComboBox: newValue -> null");
				return;
			}
			if (oldValue == null)
				return;
			
			PSWG.PREFS.put("theme", newValue);
			mainController.getPswg().loadFxmls();
			mainController.getPswg().loadTheme(newValue);
			Platform.runLater(() -> {
				ModalController modal = (ModalController)mainController.getPswg().getControllers().get("modal");
				modal.loadComponent((SettingsController)mainController.getPswg().getControllers().get("settings"));
				// fix
				//themeDisplay.queueString("Theme set");
			});
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
		
		Preferences loginServersNode = PSWG.PREFS.node("login_servers");
		loginServerComboBox.getItems().clear();
		try {
			for (String server : loginServersNode.keys())
				loginServerComboBox.getItems().add(server);
		} catch (BackingStoreException e) {
			PSWG.log("Error loading loginServers from prefs");
			e.printStackTrace();
		}
		
		loginServerComboBox.setValue(PSWG.PREFS.get("login_server", Manager.PSWG_LOGIN_SERVER_NAME));
	}
	
	public void initSetupLoginServerSettings()
	{
		// fix
		String hostnamePattern = "[0-9a-zA-Z\\.-]*";
		String portPattern = "^[0-9]{0,6}$";
		
		ChangeListener<String> hostnameTextChangeListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Pattern.matches(hostnamePattern, newValue)) {
					mainController.getManager().setLoginServerHostname(loginServerComboBox.getValue(), newValue);
				} else {
					hostnameTextField.setText(oldValue);
				}
			}
		};
		
		ChangeListener<String> portTextChangeListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Pattern.matches(portPattern, newValue))
					mainController.getManager().setLoginServerPort(loginServerComboBox.getValue(), newValue);
				else
					portTextField.setText(oldValue);
			}
		};
		
		ChangeListener<String> statusPortTextChangeListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Pattern.matches(portPattern, newValue))
					mainController.getManager().setLoginServerStatusPort(loginServerComboBox.getValue(), newValue);
				else
					statusPortTextField.setText(oldValue);
			}
		};
		
		// hostnameTextField.setTextFormatter(textFormatter);
		loginServerLockedCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {

				hostnameTextField.setDisable(true);
				hostnameTextField.textProperty().removeListener(hostnameTextChangeListener);
				hostnameTextField.textProperty().bind(mainController.getManager().getLoginServerHost());

				portTextField.setDisable(true);
				portTextField.textProperty().removeListener(portTextChangeListener);
				portTextField.textProperty().bind(mainController.getManager().getLoginServerPlayPort());

				statusPortTextField.setDisable(true);
				statusPortTextField.textProperty().removeListener(statusPortTextChangeListener);
				statusPortTextField.textProperty().bind(mainController.getManager().getLoginServerPingPort());
				
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
		
		loginServerComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null)
				return;
			
			PSWG.log("Setting server: " + newValue);
			
			loginServerLockedCheckBox.setSelected(true);
			if (newValue.equals(Manager.PSWG_LOGIN_SERVER_NAME)) {
				loginServerLockedCheckBox.setDisable(true);
				removeLoginServerButton.setDisable(true);
			} else if (oldValue != null && oldValue.equals(Manager.PSWG_LOGIN_SERVER_NAME)) {
				loginServerLockedCheckBox.setDisable(false);
				removeLoginServerButton.setDisable(false);
			}
			
			mainController.getManager().setLoginServerByName(newValue);
		});
		refreshLoginServerComboBox();
		
		pingButton.setOnAction((e) -> {
			mainController.getManager().pingLoginServer();
		});
		
		mainController.getManager().getPingService().setOnSucceeded((e) -> {
			String result = (String)e.getSource().getValue();
			PSWG.log("pingerOut: " + result);
			//Platform.runLater(() -> {
				pingDisplay.queueString("" + result);
			//});
		});

		mainController.getManager().getPingService().setOnRunning((e) -> {
			ProgressIndicator progressIndicator = new ProgressIndicator();
			progressIndicator.setManaged(false);
			progressIndicator.resize(15, 15);
			//Platform.runLater(() -> {
				pingDisplay.queueNode(progressIndicator);
			//});
		});
		
		addLoginServerButton.setOnAction((e) -> {
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle("Add Server");
			dialog.setHeaderText("Please enter a name for the server.");
			dialog.setContentText("Server name: ");
			Optional<String> result = dialog.showAndWait();
			result.ifPresent(name -> {
				mainController.getManager().addLoginServer(name);
				refreshLoginServerComboBox();
				loginServerComboBox.setValue(name);
				loginServerLockedCheckBox.setSelected(false);
			});
		});
		
		removeLoginServerButton.setOnAction((e) -> {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Remove Server");
			alert.setHeaderText("Are you sure you want to remove this server?");
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				String val = loginServerComboBox.getValue();
				loginServerComboBox.getSelectionModel().select(0);
				mainController.getManager().removeLoginServer(val);
				refreshLoginServerComboBox();
			}
		});
	}
	
	public void initSetupFolderSettings()
	{
		settingsSwgFolderButton.textProperty().bind(mainController.getManager().getSwgFolder());
		settingsSwgFolderTooltip.textProperty().bind(mainController.getManager().getSwgFolder());
		settingsPswgFolderButton.textProperty().bind(mainController.getManager().getPswgFolder());
		settingsPswgFolderTooltip.textProperty().bind(mainController.getManager().getPswgFolder());
		
		mainController.getManager().getSwgFolder().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(""))
				swgFolderDisplay.queueString(MainController.XMARK);
			else
				swgFolderDisplay.queueString(MainController.CHECKMARK);
		});
		
		mainController.getManager().getPswgFolder().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(""))
				pswgFolderDisplay.queueString(MainController.XMARK);
			else
				pswgFolderDisplay.queueString(MainController.CHECKMARK);
				
		});
		
		settingsSwgFolderButton.setOnAction((e) -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select SWG folder");
			File file = directoryChooser.showDialog(mainController.getStage());
			if (file == null || !file.isDirectory())
				return;
			
			String swgPath =  file.getAbsolutePath();
			mainController.getManager().scanSwg(swgPath);
		});
		
		settingsPswgFolderButton.setOnAction((e) -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select ProjectSWG folder");
			File file = directoryChooser.showDialog(mainController.getStage());
			if (file == null || !file.isDirectory())
				return;

			String pswgPath = file.getAbsolutePath();
			mainController.getManager().getPswgFolder().set(pswgPath);
		});
	}
	
	public void initDeveloperPane()
	{
		localhostCheckBox.setSelected(PSWG.PREFS.getBoolean("localhost", false));
		localhostCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.putBoolean("localhost", newValue);
		});
		
		debugCheckBox.setSelected(PSWG.PREFS.getBoolean("debug", false));
		debugCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.putBoolean("debug", newValue);
			openOnLaunchButton.setDisable(!newValue);
		});
		
		openOnLaunchButton.setSelected(PSWG.PREFS.getBoolean("open_on_launch", false));
		openOnLaunchButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.putBoolean("open_on_launch", newValue);
		});
		
		mainController.getManager().getPswgFolder().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(""))
				deleteGameProfilesButton.setDisable(true);
			else
				deleteGameProfilesButton.setDisable(false);
		});
		
		deleteGameProfilesButton.setOnAction((e) -> {
			String pswgFolder = mainController.getManager().getPswgFolder().getValue();
			if (pswgFolder.equals(""))
				return;
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Delete game profiles");
			alert.setHeaderText(String.format("This will remove the %s\\profiles' folder.", pswgFolder));
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK)
				try {
					Manager.removeRecursive(Paths.get(pswgFolder + "\\profiles"));
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
		wineBinaryButton.textProperty().bind(mainController.getManager().getWineBinary());
		
		wineBinaryButton.setOnAction((e) -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Select SWG folder");
			File file = fileChooser.showOpenDialog(mainController.getStage());
			if (file == null || !file.isFile())
				return;
			
			String swgPath =  file.getAbsolutePath();
			PSWG.PREFS.put("wine_bin", swgPath);
		});
		
		wineArgumentsTextField.setText(PSWG.PREFS.get("wine_arguments", ""));
		wineArgumentsTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.put("wine_arguments", newValue);
		});
		
		wineEnvironmentVariablesTextField.setText(PSWG.PREFS.get("wine_environment_variables", ""));
		wineEnvironmentVariablesTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.put("wine_environment_variables", newValue);
		});
	}
	
	public void initInfoPane()
	{
		pswgHyperlink.setOnAction((e) -> {
			try {
				java.awt.Desktop.getDesktop().browse(new URI(PSWG.PSWG_URL));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
	
		licenseText.setText(
			"This file is part of ProjectSWG Launchpad.\n" +
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
		File file = new File(PSWG.THEMES_FOLDER);
		if (!file.isDirectory()) {
			PSWG.log("Folder not found: " + PSWG.THEMES_FOLDER);
			return;
		}
		
		String[] subdirs = file.list((current, name) -> {
			return new File(current, name).isDirectory();
		});

		ArrayList<String> themeList = new ArrayList<>();
		themeList.add("Default");

		for (String dir : subdirs) {
			file = new File(PSWG.THEMES_FOLDER + "/" + dir + "/" + PSWG.CSS_NAME);
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
	}
	
	@Override
	public Parent getRoot()
	{
		return settingsRoot;
	}
}
