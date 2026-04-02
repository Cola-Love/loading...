import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class KuGouBatchDecoder {
    private static final int HEADER_LEN = 1024;
    private static final int OWN_KEY_LEN = 17;
    private static final long PUB_KEY_LEN = 1170494464L;
    private static final int PUB_KEY_MAGNIFICATION = 16;
    private static final byte[] MAGIC_HEADER = {0x7c, (byte)0xd5, 0x32, (byte)0xeb, (byte)0x86, 0x02, 0x7f, 0x4b, (byte)0xa8, (byte)0xaf, (byte)0xa6, (byte)0x8e, 0x0f, (byte)0xff, (byte)0x99, 0x14, 0x00, 0x04, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00};
    private static final int[] PUB_KEY_MEND = {0xB8, 0xD5, 0x3D, 0xB2, 0xE9, 0xAF, 0x78, 0x8C, 0x83, 0x33, 0x71, 0x51, 0x76, 0xA0, 0xCD, 0x37, 0x2F, 0x3E, 0x35, 0x8D, 0xA9, 0xBE, 0x98, 0xB7, 0xE7, 0x8C, 0x22, 0xCE, 0x5A, 0x61, 0xDF, 0x68, 0x69, 0x89, 0xFE, 0xA5, 0xB6, 0xDE, 0xA9, 0x77, 0xFC, 0xC8, 0xBD, 0xBD, 0xE5, 0x6D, 0x3E, 0x5A, 0x36, 0xEF, 0x69, 0x4E, 0xBE, 0xE1, 0xE9, 0x66, 0x1C, 0xF3, 0xD9, 0x02, 0xB6, 0xF2, 0x12, 0x9B, 0x44, 0xD0, 0x6F, 0xB9, 0x35, 0x89, 0xB6, 0x46, 0x6D, 0x73, 0x82, 0x06, 0x69, 0xC1, 0xED, 0xD7, 0x85, 0xC2, 0x30, 0xDF, 0xA2, 0x62, 0xBE, 0x79, 0x2D, 0x62, 0x62, 0x3D, 0x0D, 0x7E, 0xBE, 0x48, 0x89, 0x23, 0x02, 0xA0, 0xE4, 0xD5, 0x75, 0x51, 0x32, 0x02, 0x53, 0xFD, 0x16, 0x3A, 0x21, 0x3B, 0x16, 0x0F, 0xC3, 0xB2, 0xBB, 0xB3, 0xE2, 0xBA, 0x3A, 0x3D, 0x13, 0xEC, 0xF6, 0x01, 0x45, 0x84, 0xA5, 0x70, 0x0F, 0x93, 0x49, 0x0C, 0x64, 0xCD, 0x31, 0xD5, 0xCC, 0x4C, 0x07, 0x01, 0x9E, 0x00, 0x1A, 0x23, 0x90, 0xBF, 0x88, 0x1E, 0x3B, 0xAB, 0xA6, 0x3E, 0xC4, 0x73, 0x47, 0x10, 0x7E, 0x3B, 0x5E, 0xBC, 0xE3, 0x00, 0x84, 0xFF, 0x09, 0xD4, 0xE0, 0x89, 0x0F, 0x5B, 0x58, 0x70, 0x4F, 0xFB, 0x65, 0xD8, 0x5C, 0x53, 0x1B, 0xD3, 0xC8, 0xC6, 0xBF, 0xEF, 0x98, 0xB0, 0x50, 0x4F, 0x0F, 0xEA, 0xE5, 0x83, 0x58, 0x8C, 0x28, 0x2C, 0x84, 0x67, 0xCD, 0xD0, 0x9E, 0x47, 0xDB, 0x27, 0x50, 0xCA, 0xF4, 0x63, 0x63, 0xE8, 0x97, 0x7F, 0x1B, 0x4B, 0x0C, 0xC2, 0xC1, 0x21, 0x4C, 0xCC, 0x58, 0xF5, 0x94, 0x52, 0xA3, 0xF3, 0xD3, 0xE0, 0x68, 0xF4, 0x00, 0x23, 0xF3, 0x5E, 0x0A, 0x7B, 0x93, 0xDD, 0xAB, 0x12, 0xB2, 0x13, 0xE8, 0x84, 0xD7, 0xA7, 0x9F, 0x0F, 0x32, 0x4C, 0x55, 0x1D, 0x04, 0x36, 0x52, 0xDC, 0x03, 0xF3, 0xF9, 0x4E, 0x42, 0xE9, 0x3D, 0x61, 0xEF, 0x7C, 0xB6, 0xB3, 0x93, 0x50};
    
    private static String musicPath = "/storage/emulated/0/Download/KuGouLite/Music/";
    private static byte[] pubKey = null;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("=== 酷狗音乐自动解码监控器 ===");
        System.out.println("监控目录: " + musicPath);
        
        // 获取脚本所在目录
        String scriptDir = System.getProperty("user.dir");
        File keyZip = new File(scriptDir, "kugou_key.zip");
        
        if (!keyZip.exists()) {
            System.err.println("错误：未找到密钥文件 " + keyZip.getPath());
            System.err.println("请将 kugou_key.zip 放在脚本同目录下");
            return;
        }
        
        try {
            pubKey = loadKey(keyZip.getPath());
            System.out.println("密钥加载成功 (来自: " + keyZip.getPath() + ")");
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running = false;
                System.out.println("\n正在关闭监控器...");
            }));
            
            // 先处理现有文件
            processExistingFiles();
            
            // 启动文件监控
            startFileWatcher();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void processExistingFiles() {
        File dir = new File(musicPath);
        if (!dir.exists()) {
            System.out.println("警告：监控目录不存在，正在创建...");
            dir.mkdirs();
        }
        
        File[] files = dir.listFiles((d, name) -> 
            name.toLowerCase().endsWith(".kgm") || name.toLowerCase().endsWith(".kgm.flac"));
        
        if (files != null && files.length > 0) {
            System.out.println("\n发现 " + files.length + " 个待处理文件，开始解码...");
            for (File f : files) {
                decodeFile(f);
            }
            System.out.println("初始文件处理完成\n");
        } else {
            System.out.println("未发现现有加密文件，进入监控模式");
        }
    }
    
    private static void startFileWatcher() throws IOException {
        Path path = Paths.get(musicPath);
        
        // 确保目录存在
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.register(watchService, 
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY);
        
        System.out.println("开始监控文件变化...");
        System.out.println("提示：按 Ctrl+C 停止监控\n");
        
        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                break;
            }
            
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                
                Path filename = (Path) event.context();
                String fileName = filename.toString().toLowerCase();
                
                // 检查是否是KGM文件
                if (fileName.endsWith(".kgm") || fileName.endsWith(".kgm.flac")) {
                    // 等待文件写入完成
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                    
                    File newFile = new File(musicPath, filename.toString());
                    if (newFile.exists() && newFile.length() > 0) {
                        decodeFile(newFile);
                    }
                }
            }
            
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
        
        watchService.close();
    }
    
    private static void decodeFile(File inFile) {
        String oldName = inFile.getName();
        String newName;
        
        // 根据后缀确定输出格式
        if (oldName.toLowerCase().endsWith(".kgm.flac")) {
            newName = oldName.substring(0, oldName.length() - 9) + ".flac";
        } else if (oldName.toLowerCase().endsWith(".kgm")) {
            newName = oldName.substring(0, oldName.length() - 4) + ".mp3";
        } else {
            newName = oldName.replace(".kgm", "");
        }
        
        File outFile = new File(musicPath, newName);
        
        // 如果输出文件已存在，添加序号
        int counter = 1;
        while (outFile.exists()) {
            String baseName = newName.substring(0, newName.lastIndexOf('.'));
            String ext = newName.substring(newName.lastIndexOf('.'));
            outFile = new File(musicPath, baseName + "_" + counter + ext);
            counter++;
        }
        
        System.out.print("[新文件] " + oldName + " -> 解码中...");
        
        try {
            decode(inFile, outFile, pubKey);
            System.out.println(" 完成 -> " + outFile.getName());
            
            // 删除原文件
            if (inFile.delete()) {
                System.out.println("  └─ 已清理原文件");
            } else {
                System.out.println("  └─ 警告：无法删除原文件");
            }
        } catch (Exception e) {
            System.err.println(" 失败: " + e.getMessage());
            if (outFile.exists()) outFile.delete();
        }
    }
    
    private static byte[] loadKey(String path) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(path))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) throw new IOException("ZIP内无内容");
            byte[] key = new byte[(int)(PUB_KEY_LEN / PUB_KEY_MAGNIFICATION)];
            int read = 0, n;
            byte[] buf = new byte[8192];
            while (read < key.length && (n = zis.read(buf)) != -1) {
                int toCopy = Math.min(n, key.length - read);
                System.arraycopy(buf, 0, key, read, toCopy);
                read += toCopy;
            }
            return key;
        }
    }
    
    private static void decode(File in, File out, byte[] pubKey) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(in, "r");
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] header = new byte[HEADER_LEN];
            raf.readFully(header);
            
            if (header[0] != MAGIC_HEADER[0]) throw new IOException("无效的KGM头");
            
            byte[] ownKey = new byte[OWN_KEY_LEN];
            System.arraycopy(header, 0x1c, ownKey, 0, 16);
            
            byte[] buffer = new byte[65536];
            int n;
            long total = 0;
            while ((n = raf.read(buffer)) != -1) {
                for (int i = 0; i < n; i++) {
                    long pos = total + i;
                    int b = buffer[i] & 0xFF;
                    int ok = (ownKey[(int)(pos % OWN_KEY_LEN)] & 0xFF) ^ b;
                    ok ^= (ok & 0x0F) << 4;
                    int pkIdx = (int)(pos / PUB_KEY_MAGNIFICATION);
                    int pkm = PUB_KEY_MEND[(int)(pos % PUB_KEY_MEND.length)] & 0xFF;
                    int pk = pkm ^ (pubKey[pkIdx] & 0xFF);
                    pk ^= (pk & 0x0F) << 4;
                    buffer[i] = (byte) (ok ^ pk);
                }
                fos.write(buffer, 0, n);
                total += n;
            }
        }
    }
}