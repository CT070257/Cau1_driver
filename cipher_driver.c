#include <linux/module.h>
#include <linux/fs.h>
#include <linux/uaccess.h>
#include <linux/cdev.h>

#define DEVICE_NAME "cipher_dev"
#define BUFFER_SIZE 1024

static int major_num;
static char kernel_buffer[BUFFER_SIZE];
static int data_len = 0;

// Thuật toán mã hóa chuyển vị (Đảo ngược)
static void transposition_cipher(char *buf, int len) {
    int i;
    char temp;
    for (i = 0; i < len / 2; i++) {
        temp = buf[i];
        buf[i] = buf[len - 1 - i];
        buf[len - 1 - i] = temp;
    }
}

static ssize_t dev_write(struct file *filep, const char __user *buffer, size_t len, loff_t *offset) {
    if (len > BUFFER_SIZE) len = BUFFER_SIZE;
    
    if (copy_from_user(kernel_buffer, buffer, len)) {
        return -EFAULT;
    }
    
    data_len = len;
    // Gọi thuật toán mã hóa ngay khi nhận dữ liệu
    transposition_cipher(kernel_buffer, data_len);
    
    *offset = 0;
    return len;
}

static ssize_t dev_read(struct file *filep, char __user *buffer, size_t len, loff_t *offset) {
    if (*offset >= data_len) return 0; // Đã đọc hết
    
    if (len > data_len - *offset) len = data_len - *offset;
    
    if (copy_to_user(buffer, kernel_buffer + *offset, len)) {
        return -EFAULT;
    }
    
    *offset += len;
    return len;
}

static int dev_open(struct inode *inodep, struct file *filep) {
    return 0;
}

static int dev_release(struct inode *inodep, struct file *filep) {
    return 0;
}

static struct file_operations fops = {
    .open = dev_open,
    .read = dev_read,
    .write = dev_write,
    .release = dev_release,
};

static int __init cipher_init(void) {
    major_num = register_chrdev(0, DEVICE_NAME, &fops);
    if (major_num < 0) return major_num;
    printk(KERN_INFO "Cipher Driver loaded. Major number: %d\n", major_num);
    return 0;
}

static void __exit cipher_exit(void) {
    unregister_chrdev(major_num, DEVICE_NAME);
    printk(KERN_INFO "Cipher Driver unloaded.\n");
}

module_init(cipher_init);
module_exit(cipher_exit);
MODULE_LICENSE("GPL");
