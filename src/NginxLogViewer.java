import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

// Nginx日志查看器的主窗口类，继承自JFrame
public class NginxLogViewer extends JFrame {
    // 界面组件
    private JTable logTable;                // 显示日志的表格
    private LogTableModel tableModel;       // 表格数据模型
    private JTextField filterIpField;       // IP过滤输入框
    private JTextField filterStatusField;   // 状态码过滤输入框
    private JTextField filterPathField;   // 状态码过滤输入框
    private List<NginxLogEntry> logEntries = new ArrayList<>(); // 存储所有日志条目
    // 文件监控相关
    private volatile boolean monitoring = false; // 监控开关标志
    private Thread monitorThread;            // 文件监控线程
    private File logfilePath;

    // 构造函数
    public NginxLogViewer() {
        initUI(); // 初始化用户界面
        autoOpenLogFile();
    }

    // 初始化用户界面组件
    private void initUI() {
        //setTitle("Nginx日志过滤器(基于IP,路径,状态码)");
        setTitle("Nginx日志查看器");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // 初始化菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("文件");
        JMenuItem openItem = new JMenuItem("打开日志文件");
        openItem.addActionListener(e -> openLogFile()); // 绑定打开文件操作
        fileMenu.add(openItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // 初始化工具栏和过滤组件
        JToolBar toolBar = new JToolBar();
        filterIpField = new JTextField(15);
        filterIpField.setToolTipText("按IP过滤");
        filterStatusField = new JTextField(5);
        filterStatusField.setToolTipText("按状态码过滤");
        filterPathField = new JTextField(5);
        filterPathField.setToolTipText("按路径过滤");
        JButton filterBtn = new JButton("过滤");
        filterBtn.addActionListener(e -> filterTable()); // 绑定过滤操作
        JButton reloadBtn = new JButton("重置");
        reloadBtn.addActionListener(e -> reloadLog()); //绑定重置操作
        toolBar.add(new JLabel("IP:"));
        toolBar.add(filterIpField);
        toolBar.add(new JLabel("路径:"));
        toolBar.add(filterPathField);
        toolBar.add(new JLabel("状态码:"));
        toolBar.add(filterStatusField);
        toolBar.add(filterBtn);
        toolBar.add(reloadBtn);
        add(toolBar, BorderLayout.NORTH); // 将工具栏放在窗口顶部

        // 初始化日志表格
        tableModel = new LogTableModel();
        logTable = new JTable(tableModel);
        add(new JScrollPane(logTable), BorderLayout.CENTER); // 带滚动条的表格

    }

    // 打开日志文件对话框
    private void openLogFile() {
        JFileChooser fileChooser = new JFileChooser();

        // 设置默认打开文件（可根据需要修改）
        // File defaultLog = new File("/var/log/nginx/access.log"); // Linux默认日志路径
        File defaultLog = new File("D:\\access.log"); // Windows路径示例

        if(defaultLog.exists()){
            fileChooser.setSelectedFile(defaultLog);
        }

        // 设置默认打开路径（可根据需要修改）
        // File defaultDir = new File("/var/log/nginx/"); // Linux默认日志路径
        File defaultDir = new File("D:\\"); // Windows路径示例

        if (defaultDir.exists() && defaultDir.isDirectory()) {
            fileChooser.setCurrentDirectory(defaultDir);
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            logfilePath = fileChooser.getSelectedFile();
            loadLogFile(logfilePath);          // 加载选定文件
        }
    }

    // 加载日志文件内容
    private void loadLogFile(File file) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                logEntries.clear();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        NginxLogEntry entry = LogParser.parseLine(line); // 解析每行日志
                        if (entry != null) {
                            logEntries.add(entry);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                tableModel.setData(logEntries); // 更新表格数据
                startMonitoring(); // 在加载完成后启动监控
            }
        };
        worker.execute();
    }

    // 启动文件监控线程
    private void startMonitoring() {
        stopMonitoring(); // 确保之前的监控已停止
        monitoring = true;
        monitorThread = new Thread(() -> {
            long lastSize = 0;
            try {
                while (monitoring) {
                    long currentSize = Files.size(logfilePath.toPath());
                    if (currentSize > lastSize) {
                        readNewLines(logfilePath.toPath(), lastSize); // 读取新增内容
                        lastSize = currentSize;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // 睡眠被中断，退出循环
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        monitorThread.start();
    }

    // 读取文件新增内容
    private void readNewLines(Path path, long lastSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            raf.seek(lastSize); // 跳转到上次读取位置
            String line;
            while ((line = raf.readLine()) != null) {
                NginxLogEntry entry = LogParser.parseLine(line);
                if (entry != null) {
                    logEntries.add(entry); // 添加新条目
                }
            }
            SwingUtilities.invokeLater(() -> tableModel.setData(logEntries)); // 更新表格
        }
    }

    // 停止文件监控
    private void stopMonitoring() {
        monitoring = false; // 先更新状态标志
        if (monitorThread != null) {
            monitorThread.interrupt(); // 再中断线程
            monitorThread = null;
        }
    }

    // 根据过滤条件更新表格数据
    private void filterTable() {

        String ipFilter = filterIpField.getText().trim().toLowerCase();
        String statusFilter = filterStatusField.getText().trim();
        String pathFilter = filterPathField.getText().trim();
        
        List<NginxLogEntry> filtered = logEntries.stream()
                .filter(entry -> ipFilter.isEmpty() || entry.getIp().toLowerCase().contains(ipFilter))
                .filter(entry -> statusFilter.isEmpty() || String.valueOf(entry.getStatusCode()).contains(statusFilter))
                .filter(entry -> pathFilter.isEmpty() || String.valueOf(entry.getPath()).contains(pathFilter))
                .toList();
        
        tableModel.setData(filtered); // 更新表格显示过滤后的数据

    }
    private File getDefaultLogFile() {
        // 尝试多个常见路径
        File[] possiblePaths = {
            new File("/var/log/nginx/access.log"),  // Linux
            new File("/usr/local/var/log/nginx/access.log"), // Mac
            new File(System.getenv("ProgramFiles") + "\\nginx\\logs\\access.log"), // Windows
            new File("D:\\access.log") // 测试文件
        };
        
        for (File path : possiblePaths) {
            if (path.exists()) {
                return path;
            }
        }
        return null; // 没有找到默认文件
    }


    private void autoOpenLogFile() {
        File defaultLog = getDefaultLogFile();
        if (defaultLog != null && defaultLog.exists()) {
            logfilePath = defaultLog;
            loadLogFile(logfilePath);
        } else {
            openLogFile(); // 回退到手动选择
        }
    }

    private void reloadLog(){
        loadLogFile(logfilePath);   
    }

   
}