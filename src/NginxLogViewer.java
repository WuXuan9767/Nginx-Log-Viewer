import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

// Nginx日志查看器的主窗口类，继承自JFrame
public class NginxLogViewer extends JFrame {
    // 界面组件
    private JTable logTable;                // 显示日志的表格
    private LogTableModel tableModel;       // 表格数据模型
    private JComboBox<String> filterTypeCombo1; // 第一个过滤类型下拉框
    private JTextField filterValueField1;       // 第一个过滤值输入框
    private JComboBox<String> filterTypeCombo2; // 第二个过滤类型下拉框
    private JTextField filterValueField2;       // 第二个过滤值输入框
    private List<NginxLogEntry> logEntries = new ArrayList<>(); // 存储所有日志条目
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
        // 修改后的工具栏
        JToolBar toolBar = new JToolBar();
        String[] filterOptions = {"IP", "路径", "状态码", "method", "userAgent"};
        
        // 第一个过滤条件组
        filterTypeCombo1 = new JComboBox<>(filterOptions);
        filterValueField1 = new JTextField(15);
        toolBar.add(new JLabel("过滤条件1:"));
        toolBar.add(filterTypeCombo1);
        toolBar.add(filterValueField1);

        // 第二个过滤条件组
        filterTypeCombo2 = new JComboBox<>(filterOptions);
        filterValueField2 = new JTextField(15);
        toolBar.add(new JLabel("过滤条件2:"));
        toolBar.add(filterTypeCombo2);
        toolBar.add(filterValueField2);

        JButton filterBtn = new JButton("过滤");
        filterBtn.addActionListener(e -> filterTable());
        JButton reloadBtn = new JButton("重置");
        reloadBtn.addActionListener(e -> reloadLog());
        
        toolBar.add(filterBtn);
        toolBar.add(reloadBtn);
        add(toolBar, BorderLayout.NORTH);

        // 初始化日志表格
        tableModel = new LogTableModel();
        logTable = new JTable(tableModel);
        add(new JScrollPane(logTable), BorderLayout.CENTER); // 带滚动条的表格

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
            }
        };
        worker.execute();
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

    // 通用过滤判断方法
    private boolean applyFilter(NginxLogEntry entry, String filterType, String filterValue) {
        if (filterValue.isEmpty()) return true;
        
        return switch (filterType) {
            case "IP" -> entry.getIp().toLowerCase().contains(filterValue);
            case "路径" -> entry.getPath().toLowerCase().contains(filterValue);
            case "状态码" -> String.valueOf(entry.getStatusCode()).contains(filterValue);
            case "method" -> entry.getMethod().toLowerCase().contains(filterValue);
            case "userAgent" -> entry.getUserAgent().toLowerCase().contains(filterValue);
            default -> true;
        };
    }


    private void reloadLog(){
        loadLogFile(logfilePath);   
    }

       // 根据过滤条件更新表格数据
       private void filterTable() {
        // 获取第一个过滤条件
        String type1 = (String) filterTypeCombo1.getSelectedItem();
        String value1 = filterValueField1.getText().trim().toLowerCase();
        
        // 获取第二个过滤条件
        String type2 = (String) filterTypeCombo2.getSelectedItem();
        String value2 = filterValueField2.getText().trim().toLowerCase();

        List<NginxLogEntry> filtered = logEntries.stream()
            .filter(entry -> applyFilter(entry, type1, value1))
            .filter(entry -> applyFilter(entry, type2, value2))
            .toList();

        tableModel.setData(filtered);
    }
}