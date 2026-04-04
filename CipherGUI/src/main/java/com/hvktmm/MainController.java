package com.hvktmm;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private ListView<String> listViewFiles;
    @FXML private Label statusLabel;

    private List<File> selectedFiles = new ArrayList<>();
    // Đường dẫn trỏ ra thư mục chứa app_manager bằng C của bạn
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
    private void handleEncrypt() { executeCommand("-enc"); }

    @FXML
    private void handleDecrypt() { executeCommand("-dec"); }

    @FXML
    private void handleEdit() {
        if (selectedFiles.size() == 1) {
            executeCommand("-edit");
        } else {
            statusLabel.setText("Lỗi: Chỉ chọn 1 file để sửa!");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // Hàm gọi tiến trình C backend
    private void executeCommand(String mode) {
        if (selectedFiles.isEmpty()) {
            statusLabel.setText("Vui lòng chọn file trước!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            ProcessBuilder pb;
            
            if (mode.equals("-edit")) {
                String targetFile = selectedFiles.get(0).getAbsolutePath();
                // Bật cửa sổ terminal Ubuntu mới để chạy nano
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
                // Mã hóa / Giải mã: Đợi C chạy xong rồi kiểm tra kết quả
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    statusLabel.setText("Xử lý thành công!");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    openFileManager(); // Mở thư mục để xem kết quả
                } else {
                    statusLabel.setText("Tiến trình C báo lỗi (Code: " + exitCode + ")");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
            } else {
                // Chỉnh sửa: Báo trạng thái và đợi quá trình đóng terminal hoàn tất
                statusLabel.setText("Đang mở cửa sổ Terminal để chỉnh sửa...");
                statusLabel.setStyle("-fx-text-fill: blue;");
                
                process.waitFor();
                statusLabel.setText("Vui lòng thao tác sửa file trên Terminal!");
                statusLabel.setStyle("-fx-text-fill: green;");
                
                // ĐÃ XÓA hàm openFileManager() ở đây để không làm phiền khi bạn đang sửa
            }

        } catch (Exception e) {
            statusLabel.setText("Lỗi khởi chạy tiến trình!");
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    // Hàm tự động gọi trình quản lý file của Ubuntu
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
