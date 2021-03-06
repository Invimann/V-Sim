/*
Copyright (C) 2018-2019 Andres Castellanos

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>
*/

package vsim.gui.components;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.controls.JFXTextField;
import vsim.gui.utils.Icons;


/** This class represents a text input dialog. */
public final class InputDialog {

  /** Escape key combination */
  private static final KeyCodeCombination ESCAPE = new KeyCodeCombination(KeyCode.ESCAPE);

  /** Enter key combination */
  private static final KeyCodeCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);

  /** If user hits enter key */
  private boolean enterPressed;
  /** user input queue */
  private final ArrayBlockingQueue<String> queue;
  /** Dialog stage */
  private Stage stage;
  /** Dialog enter button */
  @FXML private JFXButton enter;
  /** Dialog text field */
  @FXML private JFXTextField text;

  /**
   * Creates an input dialog with a default title.
   */
  public InputDialog() throws IOException {
    this.stage = new Stage();
    this.queue = new ArrayBlockingQueue<String>(1);
    this.stage.initModality(Modality.APPLICATION_MODAL);
    this.stage.getIcons().add(Icons.getFavicon());
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/InputDialog.fxml"));
    loader.setController(this);
    Parent root = loader.load();
    JFXDecorator decorator = new JFXDecorator(stage, root, false, false, false);
    decorator.setGraphic(Icons.getImage("logo"));
    this.enterPressed = false;
    this.stage.setResizable(false);
    Scene scene = new Scene(decorator, 304, 150);
    scene.getStylesheets().addAll(getClass().getResource("/css/jfoenix-fonts.css").toExternalForm(),
        getClass().getResource("/css/vsim-fonts.css").toExternalForm(),
        getClass().getResource("/css/vsim.css").toExternalForm());
    this.stage.setScene(scene);
    // enter actions
    this.enter.setOnAction(e -> this.enter());
    this.enter.setOnKeyPressed(e -> {
      if (InputDialog.ENTER.match(e))
        this.enter();
    });
    this.text.setOnKeyPressed(e -> {
      if (InputDialog.ENTER.match(e))
        this.enter();
    });
    // stage actions
    this.stage.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
      if (InputDialog.ESCAPE.match(e))
        this.cancel();
    });
  }

  /** Cancel action. */
  private void cancel() {
    this.enterPressed = false;
    this.stage.hide();
  }

  /** Enter action. */
  private void enter() {
    this.enterPressed = true;
    this.stage.hide();
  }

  /**
   * Shows input dialog and returns user input text.
   *
   * @param title stage title
   * @return user input text
   */
  public String getInput(String title) {
    Platform.runLater(() -> {
      // clean text field
      this.text.setText("");
      this.enterPressed = false;
      // request input focus
      this.stage.setTitle(title);
      this.text.requestFocus();
      this.stage.showAndWait();
      if (this.enterPressed)
        this.queue.offer(this.text.getText());
      else
        this.queue.offer("");
    });
    try {
      return this.queue.take();
    } catch (InterruptedException e) {
      return "";
    } finally {
      this.queue.clear();
    }
  }

}
