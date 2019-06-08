package ua.patiy.yevgen.codechecker;

import com.sun.javafx.css.StyleManager;

import javafx.application.Application;
import javafx.stage.Stage;

@SuppressWarnings("restriction")
public class Main extends Application {
    private static CodeChecker codeWorker = CodeChecker.getInstance();

    @Override
    public void start(Stage primaryStage) throws Exception {
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        StyleManager.getInstance().addUserAgentStylesheet("/css/checker.css");
        codeWorker.setMainWindow(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
