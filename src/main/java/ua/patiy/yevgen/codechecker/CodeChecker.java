package ua.patiy.yevgen.codechecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import ua.patiy.yevgen.codechecker.workers.FileData;
import ua.patiy.yevgen.codechecker.workers.FileWorker;
import ua.patiy.yevgen.codechecker.workers.OSValidator;
import ua.patiy.yevgen.codechecker.workers.OSValidator.OS;

public class CodeChecker implements FileVisitor<Path> {
    private static CodeChecker codeChecker;
    private final FileWorker fw = new FileWorker();
    private final OS os = new OSValidator().getOs();
    private List<FileData> fileList = new ArrayList<FileData>();
    private final List<CheckBox> extensions = Arrays.asList(new CheckBox(".java"), new CheckBox(".xml"),
            new CheckBox(".sql"), new CheckBox(".html"), new CheckBox(".c"), new CheckBox(".cpp"), new CheckBox(".php"),
            new CheckBox(".py"));
    private File selectedDirectory;
    private int fileCounter;
    private long lineCounter;
    private boolean hasTabsInList;
    private ObservableList<FileData> dataView;
    private BorderPane root = new BorderPane();
    private FlowPane topPane = new FlowPane();
    private Label filesLabel = new Label();
    private Label linesLabel = new Label();
    private Label filesToCheckLabel = new Label();
    private Button sourceFolderButton = new Button();
    private Button fixAllButton = new Button();
    private Locale locale = new Locale("en", "US");
    private ResourceBundle messages = ResourceBundle.getBundle("properties/messages", locale);
    private String contentTypeString;
    private String modificationTimeString;
    private String ownerString;
    private String permissionsString;
    private String sizeString;
    private final int mainW = 1024;
    private final int mainH = 600;
    private final int viewW = 800;
    private final int viewH = 600;
    private static final String[] KEYWORD = new String[] { "abstract", "assert", "boolean", "break", "byte", "case",
            "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
            "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while" };
    private static final String[] HIGHLIGHT = new String[] { String.valueOf('\u0009') };
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORD) + ")\\b";
    private static final String HIGHLIGHT_PATTERN = "(" + String.join("|", HIGHLIGHT) + ")\\b";
    private static final Pattern PATTERN = Pattern
            .compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + "|(?<HIGHLIGHT>" + HIGHLIGHT_PATTERN + ")");

    private CodeChecker() {
    }

    protected static CodeChecker getInstance() {
        if (codeChecker == null) {
            codeChecker = new CodeChecker();
        }
        return codeChecker;
    }

    private void handleException(Exception e) {
        Alert alert = new Alert(AlertType.ERROR);
        e.printStackTrace();
        alert.setTitle(messages.getString("error"));
        alert.setHeaderText(messages.getString("excMessage"));
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    private String getToolTipText(String filename) {
        File file = new File(filename);
        String result = file.getName() + System.lineSeparator() + "------" + System.lineSeparator();
        try {
            contentTypeString = messages.getString("contentType");
            modificationTimeString = messages.getString("modificationTime");
            ownerString = messages.getString("owner");
            permissionsString = messages.getString("permissions");
            sizeString = messages.getString("size");

            result += contentTypeString + " " + fw.getFileType(file) + System.lineSeparator();
            result += modificationTimeString + " " + fw.getFileTime(file) + System.lineSeparator();
            if ((os == OS.MAC) || (os == OS.UNIX) || (os == OS.SOLARIS)) {
                Path currentFilePath = Paths.get(file.getAbsolutePath());
                result += ownerString + " " + Files.getOwner(currentFilePath, LinkOption.NOFOLLOW_LINKS)
                        + System.lineSeparator();
                result += permissionsString + " "
                        + fw.getPermissions(Files.getPosixFilePermissions(currentFilePath, LinkOption.NOFOLLOW_LINKS))
                        + System.lineSeparator();
            }
            result += "------" + System.lineSeparator();
            result += sizeString + " " + file.length();
        } catch (IOException e) {
            handleException(e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private TableView<FileData> getTableData() {
        TableView<FileData> tableView = new TableView<FileData>();

        tableView.setRowFactory(tv -> {
            final TableRow<FileData> row = new TableRow<>();
            final MenuItem mi1 = new MenuItem(messages.getString("mi1"));
            mi1.setOnAction(event -> {
                if (row.getItem().isTab()) {
                    try {
                        fw.fixTabs(Paths.get(row.getItem().getFileName()));
                    } catch (IOException e) {
                        handleException(e);
                    }
                    updateView();
                }
            });

            final MenuItem mi2 = new MenuItem(messages.getString("mi2"));
            mi2.setOnAction(event -> {
                setViewFileWindow(row.getItem());
            });

            final MenuItem mi3 = new MenuItem(messages.getString("mi2"));
            mi3.setOnAction(event -> {
                setViewFileWindow(row.getItem());
            });

            final ContextMenu menuWhenTabs = new ContextMenu();
            menuWhenTabs.getItems().add(mi1);
            menuWhenTabs.getItems().add(mi2);
            final ContextMenu menuWhenNoTabs = new ContextMenu();
            menuWhenNoTabs.getItems().add(mi3);

            // show proper context menu for proper rows
            row.emptyProperty().addListener((observable, wasEmpty, isEmpty) -> {
                if (isEmpty) {
                    row.setContextMenu(null);
                } else {
                    if (row.getItem().isTab()) {
                        row.setContextMenu(menuWhenTabs);
                    } else {
                        row.setContextMenu(menuWhenNoTabs);
                    }
                }
            });
            row.hoverProperty().addListener((observable) -> {
                final FileData line = row.getItem();
                Tooltip tt = new Tooltip();
                if (line != null) {
                    tt.setText(getToolTipText(line.getFileName()));
                    row.setTooltip(tt);
                }
                if (row.isHover() && line != null) {
                    if (line.isTab()) {
                        row.setId("rowTabTrue");
                    } else {
                        row.setId("rowTabFalse");
                    }
                } else {
                    row.setId("rowDefault");
                }
            });
            return row;
        });
        dataView = FXCollections.observableArrayList(fileList);

        TableColumn<FileData, Boolean> tabsCol = new TableColumn<FileData, Boolean>("TABS");
        tabsCol.setCellFactory(column -> new TableCell<FileData, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item ? messages.getString("yes") : messages.getString("no"));
                    if (item) {
                        setId("tabTrue");
                    } else {
                        setId("tabFalse");
                    }
                }
            }
        });
        tabsCol.setCellValueFactory(new PropertyValueFactory<FileData, Boolean>("tab"));

        TableColumn<FileData, String> filenameCol = new TableColumn<FileData, String>(
                hasTabsInList ? messages.getString("fullPathExt") : messages.getString("fullPath"));
        filenameCol.setCellValueFactory(new PropertyValueFactory<FileData, String>("fileName"));

        TableColumn<FileData, Long> linesCol = new TableColumn<FileData, Long>(messages.getString("lines"));
        linesCol.setCellFactory(column -> new TableCell<FileData, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setId("linesCol");
                    setText(item + " ");
                }
            }
        });
        linesCol.setCellValueFactory(new PropertyValueFactory<FileData, Long>("lines"));

        tableView.getColumns().addAll(tabsCol, filenameCol, linesCol);
        tableView.getSortOrder().add(tabsCol);
        tableView.getSortOrder().add(filenameCol);
        tableView.getSortOrder().add(linesCol);
        tabsCol.prefWidthProperty().bind(tableView.widthProperty().multiply(0.05));
        tabsCol.setResizable(false);
        filenameCol.prefWidthProperty().bind(tableView.widthProperty().multiply(0.85));
        filenameCol.setResizable(false);
        linesCol.prefWidthProperty().bind(tableView.widthProperty().multiply(0.10));
        linesCol.setResizable(false);
        tableView.setItems(dataView);
        tableView.setPlaceholder(new Label(messages.getString("noFiles")));

        return tableView;
    }

    private void updateView() {
        if (selectedDirectory != null) {
            fileCounter = 0;
            lineCounter = 0;
            if (!fileList.isEmpty()) {
                fileList.clear();
            }
            processDirectory(selectedDirectory);
            root.setCenter(getTableData());
            if (hasTabsInList) {
                fixAllButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/warning.png"))));
                fixAllButton.setOnAction(event -> {
                });
                if (!topPane.getChildren().contains(fixAllButton)) {
                    topPane.getChildren().add(fixAllButton);
                }
            } else {
                if (topPane.getChildren().contains(fixAllButton)) {
                    topPane.getChildren().remove(fixAllButton);
                }
            }
            filesLabel.setText(messages.getString("totalFiles") + " " + fileCounter);
            linesLabel.setText(messages.getString("totalLines") + " " + lineCounter);
        }
    }

    private static StyleSpans<Collection<String>> highlight(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass = matcher.group("KEYWORD") != null ? "keyword"
                    : matcher.group("HIGHLIGHT") != null ? "highlight" : null;
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private void setViewFileWindow(FileData fileData) {
        Path file = Paths.get(fileData.getFileName());
        Stage stage = new Stage();
        CodeArea textArea = new CodeArea();
        FlowPane bottomPane = new FlowPane();
        bottomPane.setAlignment(Pos.CENTER);
        bottomPane.setId("bottomPane");
        BorderPane view = new BorderPane();
        view.setPadding(new Insets(10));

        textArea.setCache(true);
        textArea.setEditable(false);
        textArea.setParagraphGraphicFactory(LineNumberFactory.get(textArea));
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            textArea.setStyleSpans(0, highlight(newText));
        });
        try (InputStream in = new FileInputStream(file.toFile());
                Reader reader = new InputStreamReader(in);
                BufferedReader buffer = new BufferedReader(reader)) {
            String line;
            while ((line = buffer.readLine()) != null) {
                textArea.appendText(line + System.lineSeparator());
            }
        } catch (FileNotFoundException e) {
            handleException(e);
        } catch (IOException e) {
            handleException(e);
        }

        view.setCenter(new StackPane(new VirtualizedScrollPane<CodeArea>(textArea)));
        try {
            bottomPane.getChildren().add(new Label(modificationTimeString + " " + fw.getFileTime(file.toFile())));
        } catch (IOException e) {
            handleException(e);
        }
        if ((os == OS.MAC) || (os == OS.UNIX) || (os == OS.SOLARIS)) {
            try {
                bottomPane.getChildren()
                        .add(new Label(ownerString + " " + Files.getOwner(file, LinkOption.NOFOLLOW_LINKS)));
                bottomPane.getChildren().add(new Label(permissionsString + " "
                        + fw.getPermissions(Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS))));
            } catch (IOException e) {
                handleException(e);
            }
        }
        bottomPane.getChildren().add(new Label(sizeString + " " + file.toFile().length()));
        bottomPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(10)));
        view.setBottom(bottomPane);
        try {
            stage.setTitle(file.getFileName().toString() + "  |  " + fw.getFileType(file.toFile()) + "  |  "
                    + messages.getString("linesSmall") + ": " + fileData.getLines());
        } catch (IOException e) {
            handleException(e);
        }
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/code.png")));
        stage.setScene(new Scene(view, viewW, viewH));
        stage.show();
    }

    protected Scene setMainWindow(Stage primaryStage) {
        ObservableList<String> languages = FXCollections.observableArrayList("English", "Українська");
        final ComboBox<String> localesComboBox = new ComboBox<String>(languages);
        localesComboBox.setValue("English");
        localesComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equalsIgnoreCase("english")) {
                locale = new Locale("en", "US");
            } else if (newValue.equalsIgnoreCase("українська")) {
                locale = new Locale("uk", "UA");
            }
            messages = ResourceBundle.getBundle("properties/messages", locale);
            sourceFolderButton.setText(messages.getString("sources"));
            fixAllButton.setText(messages.getString("fixAll"));
            filesToCheckLabel.setText(messages.getString("files"));
            primaryStage.setTitle(messages.getString("title"));
            updateView();
        });

        topPane.setId("topPane");
        FlowPane bottomPane = new FlowPane();
        bottomPane.setId("bottomPane");
        Scene scene = new Scene(root, mainW, mainH);
        sourceFolderButton.setText(messages.getString("sources"));
        sourceFolderButton.setOnAction(event -> {
            selectedDirectory = (new DirectoryChooser()).showDialog(primaryStage);
            primaryStage.setTitle(selectedDirectory == null ? primaryStage.getTitle()
                    : messages.getString("title") + ": " + selectedDirectory);
            updateView();
        });
        fixAllButton.setText(messages.getString("fixAll"));
        bottomPane.setAlignment(Pos.CENTER);
        filesToCheckLabel.setText(messages.getString("files"));
        bottomPane.getChildren().add(filesToCheckLabel);
        extensions.forEach(extension -> {
            if ((extension.getText().equalsIgnoreCase(".java")) || (extension.getText().equalsIgnoreCase(".xml"))) {
                extension.setSelected(true);
            }
            extension.setOnAction(event -> updateView());
            FlowPane.setMargin(extension, new Insets(20));
            bottomPane.getChildren().add(extension);
        });

        topPane.setAlignment(Pos.CENTER);
        topPane.getChildren().addAll(localesComboBox, filesLabel, sourceFolderButton, linesLabel);
        topPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(20)));
        root.setTop(topPane);
        root.setBottom(bottomPane);
        root.setId("mainWindow");

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/img/code.png")));
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.setTitle(messages.getString("title"));
        primaryStage.show();
        return scene;
    }

    private void processDirectory(File dir) {
        hasTabsInList = false;
        try {
            Files.walkFileTree(Paths.get(dir.getAbsolutePath()), this);
        } catch (IOException e) {
            handleException(e);
        }
    }

    private boolean matchExtension(String fileName) {
        for (CheckBox extension : extensions) {
            if (extension.isSelected() && fileName.toLowerCase().endsWith(extension.getText())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (matchExtension(file.getFileName().toString())) {
            FileData tableItem = new FileData();
            tableItem.setTab(fw.hasTabs(file));
            tableItem.setFileName(file.toFile().getCanonicalPath());
            tableItem.setLines(fw.countLines(file));
            if (tableItem.isTab()) {
                hasTabsInList = true;
            }
            fileList.add(tableItem);
            fileCounter++;
            lineCounter += fw.countLines(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}
