// LogTableModel.java
import javax.swing.table.AbstractTableModel;
import java.util.List;

public class LogTableModel extends AbstractTableModel {
    private List<NginxLogEntry> data;
    private final String[] columns = {"IP", "时间", "方法", "路径", "状态码", "大小", "Referer", "User Agent"};

    public void setData(List<NginxLogEntry> newData) {
        this.data = newData;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        NginxLogEntry entry = data.get(row);
        return switch (col) {
            case 0 -> entry.getIp();
            case 1 -> entry.getTime();
            case 2 -> entry.getMethod();
            case 3 -> entry.getPath();
            case 4 -> entry.getStatusCode();
            case 5 -> entry.getBodyBytesSent();
            case 6 -> entry.getReferer();
            case 7 -> entry.getUserAgent();
            default -> null;
        };
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }
}