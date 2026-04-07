package com.hvktmm;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private ListView<String> listViewFiles;
    @FXML private Label statusLabel;

    private List<File> selectedFiles = new ArrayList<>();
    private final String WORKING_DIR = System.getProperty("user.home") + "/Btap_cuoi_ki_yc1";

    @FXML
    private void handleSelectFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file để xử lý");
        List<File> files = fileChooser.showOpenMultipleDialog(listViewFiles.getScene().getWindow());

        if (files != null) {
            selectedFiles.clear();
            listViewFiles.getItems().clear();
            selectedFiles.addAll(files);
            for (File f : files) listViewFiles.getItems().add(f.getName());
            statusLabel.setText("Đã chọn " + files.size() + " file.");
            statusLabel.setStyle("-fx-text-fill: black;");
        }
    }

    @FXML
    private void handleSelectFolder() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Chọn thư mục để xử lý");
        File dir = dirChooser.showDialog(listViewFiles.getScene().getWindow());

        if (dir != null) {
            selectedFiles.clear();
            listViewFiles.getItems().clear();
            selectedFiles.add(dir);
            listViewFiles.getItems().add("[Thư mục] " + dir.getName());
            statusLabel.setText("Đã chọn thư mục: " + dir.getName());
            statusLabel.setStyle("-fx-text-fill: black;");
        }
    }

    @FXML
    private void handleEncrypt() { executeCommand("-enc"); }

    @FXML
    private void handleDecrypt() { executeCommand("-dec"); }

    @FXML
    private void handleEdit() {
        if (selectedFiles.size() == 1) {
            executeCommand("-edit");
        } else {
            statusLabel.setText("Lỗi: Chỉ chọn 1 đối tượng để sửa!");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void executeCommand(String baseMode) {
        if (selectedFiles.isEmpty()) {
            statusLabel.setText("Vui lòng chọn đối tượng trước!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        String mode = baseMode;
        boolean isDir = selectedFiles.get(0).isDirectory();
        boolean isTarEnc = selectedFiles.get(0).getName().endsWith(".tar.enc");

        // Chuyển đổi lệnh nếu đối tượng là thư mục hoặc file nén của thư mục
        if (baseMode.equals("-enc") && isDir) mode = "-enc-dir";
        else if (baseMode.equals("-dec") && isTarEnc) mode = "-dec-dir";

        // Chặn chỉnh sửa nếu là thư mục
        if (baseMode.equals("-edit") && (isDir || isTarEnc)) {
            statusLabel.setText("Lỗi: Không thể dùng Nano để sửa trực tiếp thư mục!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            ProcessBuilder pb;
            
            if (mode.equals("-edit")) {
                String targetFile = selectedFiles.get(0).getAbsolutePath();
                pb = new ProcessBuilder("gnome-terminal", "--", "bash", "-c", 
                                        "./app_manager -edit '" + targetFile + "'; echo 'Xong! Bấm Enter để đóng...'; read");
            } else {
                List<String> command = new ArrayList<>();
                command.add("./app_manager");
                command.add(mode);
                for (File f : selectedFiles) command.add(f.getAbsolutePath());
                pb = new ProcessBuilder(command);
            }

            pb.directory(new File(WORKING_DIR));
            Process process = pb.start();
            
            if (!mode.equals("-edit")) {
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    statusLabel.setText("Xử lý thành công!");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    openFileManager(); 
                } else {
                    statusLabel.setText("Tiến trình C báo lỗi (Code: " + exitCode + ")");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
            } else {
                statusLabel.setText("Đang mở cửa sổ Terminal để chỉnh sửa...");
                statusLabel.setStyle("-fx-text-fill: blue;");
                
                process.waitFor();
                statusLabel.setText("Vui lòng thao tác sửa file trên Terminal!");
                statusLabel.setStyle("-fx-text-fill: green;");
            }

        } catch (Exception e) {
            statusLabel.setText("Lỗi khởi chạy tiến trình!");
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    private void openFileManager() {
        if (selectedFiles.isEmpty()) return;
        try {
            String parentDir = selectedFiles.get(0).getParent();
            Runtime.getRuntime().exec(new String[]{"xdg-open", parentDir});
        } catch (Exception e) {
            System.out.println("Không thể mở File Manager: " + e.getMessage());
        }
    }
}
