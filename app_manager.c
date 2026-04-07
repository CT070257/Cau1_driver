#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>

#define DRIVER_PATH "/dev/cipher_dev"
#define BUFFER_SIZE 1024

int process_file(const char *input_file, const char *output_file, int fd_driver) {
    int fd_in = open(input_file, O_RDONLY);
    if (fd_in < 0) {
        printf("  [Lỗi] Không thể mở file đầu vào: %s\n", input_file);
        return -1;
    }

    int fd_out = open(output_file, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd_out < 0) {
        printf("  [Lỗi] Không thể tạo file đích: %s\n", output_file);
        close(fd_in);
        return -1;
    }

    char buffer[BUFFER_SIZE];
    ssize_t bytes_read;

    while ((bytes_read = read(fd_in, buffer, sizeof(buffer))) > 0) {
        write(fd_driver, buffer, bytes_read);
        char cipher_buffer[BUFFER_SIZE];
        read(fd_driver, cipher_buffer, bytes_read);
        write(fd_out, cipher_buffer, bytes_read);
    }

    close(fd_in);
    close(fd_out);
    return 0;
}

int edit_file(const char *target_file, int fd_driver) {
    const char *tmp_file = "/tmp/.secret_temp.txt"; 
    printf("  -> Giải mã nội dung vào bộ nhớ tạm...\n");
    if (process_file(target_file, tmp_file, fd_driver) != 0) return -1;

    printf("  -> Mở trình soạn thảo (nano)...\n");
    sleep(1); 

    char cmd[256];
    snprintf(cmd, sizeof(cmd), "nano %s", tmp_file);
    if (system(cmd) == -1) {
        printf("  [Lỗi] Không thể mở trình soạn thảo!\n");
        remove(tmp_file);
        return -1;
    }

    printf("  -> Đang mã hóa lại nội dung mới...\n");
    if (process_file(tmp_file, target_file, fd_driver) != 0) {
        printf("  [Lỗi] Quá trình mã hóa lại thất bại!\n");
        return -1;
    }

    remove(tmp_file);
    return 0;
}

int main(int argc, char *argv[]) {
    if (argc < 3) {
        printf("Sử dụng: %s <chế_độ> <đối_tượng>\n", argv[0]);
        return -1;
    }

    char *mode = argv[1];
    int fd_driver = open(DRIVER_PATH, O_RDWR);
    if (fd_driver < 0) {
        perror("Lỗi mở thiết bị driver (/dev/cipher_dev)");
        return -1;
    }

    printf("===========================================\n");
    printf(" Trình quản lý bảo mật File & Thư mục\n");
    printf("===========================================\n");

    for (int i = 2; i < argc; i++) {
        char *input_file = argv[i];
        
        // Cấp bộ đệm 4200 để chứa thoải mái mọi tiền tố nối vào file
        char output_file[4200]; 

        if (strcmp(mode, "-enc") == 0) {
            snprintf(output_file, sizeof(output_file), "%s.enc", input_file);
            printf("[MÃ HÓA FILE] %s -> %s\n", input_file, output_file);
            if (process_file(input_file, output_file, fd_driver) == 0) printf("  -> Hoàn tất!\n");
        } 
        else if (strcmp(mode, "-dec") == 0) {
            char *last_slash = strrchr(input_file, '/');
            if (last_slash != NULL) {
                int path_len = last_slash - input_file + 1;
                snprintf(output_file, sizeof(output_file), "%.*sdec_%s", path_len, input_file, last_slash + 1);
            } else {
                snprintf(output_file, sizeof(output_file), "dec_%s", input_file);
            }
            printf("[GIẢI MÃ FILE] %s -> %s\n", input_file, output_file);
            if (process_file(input_file, output_file, fd_driver) == 0) printf("  -> Hoàn tất!\n");
        } 
        else if (strcmp(mode, "-edit") == 0) {
            printf("[CHỈNH SỬA] File bảo mật: %s\n", input_file);
            if (edit_file(input_file, fd_driver) == 0) printf("  -> Đã lưu an toàn!\n");
        }
        else if (strcmp(mode, "-enc-dir") == 0) {
            char tar_file[4200];
            snprintf(tar_file, sizeof(tar_file), "%s.tar", input_file);
            snprintf(output_file, sizeof(output_file), "%s.tar.enc", input_file);
            
            printf("[MÃ HÓA THƯ MỤC] %s -> %s\n", input_file, output_file);
            
            // Lệnh chứa 2 đường dẫn 4200, cấp hẳn 13000 cho dư dả
            char cmd[13000];
            snprintf(cmd, sizeof(cmd), "tar -cf \"%s\" -C \"%s\" .", tar_file, input_file);
            if (system(cmd) == 0) {
                if (process_file(tar_file, output_file, fd_driver) == 0) {
                    printf("  -> Đã mã hóa và đóng gói thành công!\n");
                }
                remove(tar_file); 
            }
        }
        else if (strcmp(mode, "-dec-dir") == 0) {
            char tar_file[4200];
            snprintf(tar_file, sizeof(tar_file), "%s_temp.tar", input_file);
            
            char input_copy[4096];
            strcpy(input_copy, input_file);
            char *ext = strstr(input_copy, ".tar.enc");
            if (ext) *ext = '\0'; 
            
            char *last_slash = strrchr(input_copy, '/');
            if (last_slash != NULL) {
                int path_len = last_slash - input_copy + 1;
                snprintf(output_file, sizeof(output_file), "%.*sdec_%s", path_len, input_copy, last_slash + 1);
            } else {
                snprintf(output_file, sizeof(output_file), "dec_%s", input_copy);
            }

            printf("[GIẢI MÃ THƯ MỤC] %s -> %s\n", input_file, output_file);
            
            if (process_file(input_file, tar_file, fd_driver) == 0) {
                // Lệnh chứa 3 đường dẫn 4200, cấp hẳn 13000 cho dư dả
                char cmd[13000];
                snprintf(cmd, sizeof(cmd), "mkdir -p \"%s\" && tar -xf \"%s\" -C \"%s\"", output_file, tar_file, output_file);
                if (system(cmd) == 0) printf("  -> Đã bung thư mục thành công!\n");
                remove(tar_file); 
            }
        }
        else {
            printf("Chế độ '%s' không hợp lệ!\n", mode);
            break;
        }
    }

    printf("-------------------------------------------\n");
    close(fd_driver);
    return 0;
}
