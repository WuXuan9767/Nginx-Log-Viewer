import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LogParser {
    // 增强正则：允许空数值字段（如响应大小可能为"-"）
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^([\\d.]+) \\S+ \\S+ \\[([^]]+)] \"(\\S+) (\\S+) \\S+\" (\\d+|-) (\\d+|-) \"([^\"]*)\" \"([^\"]*)\""
    );
    
    public static NginxLogEntry parseLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (!matcher.matches()) return null; // 使用 matches() 而非 find()

        try {
            // 处理数值字段（状态码、响应大小）
            int statusCode = Integer.parseInt(matcher.group(5));
            long responseSize = Long.parseLong(matcher.group(6));

            return new NginxLogEntry(
                matcher.group(1),         // IP
                matcher.group(2),         // 时间戳
                matcher.group(3),         // 方法
                matcher.group(4),         // 路径
                statusCode,                     // 状态码
                responseSize,                   // 响应大小
                matcher.group(7),         // Referer
                matcher.group(8)          // UserAgent
            );

        } catch (Exception e) {
            System.err.println("未知错误: " + e.getClass().getSimpleName() + " - " + line);
            return null;
        }
    }
}