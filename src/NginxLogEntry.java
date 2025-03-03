public final class NginxLogEntry {
    private final String ip;
    private final String time;
    private final String method;
    private final String path;
    private final int statusCode;
    private final long bodyBytesSent;
    private final String referer;
    private final String userAgent;

    public NginxLogEntry(
            String ip,
            String time,
            String method,
            String path,
            int statusCode,
            long bodyBytesSent,
            String referer,
            String userAgent
    ) {
        this.ip = ip;
        this.time = time;
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.bodyBytesSent = bodyBytesSent;
        this.referer = referer;
        this.userAgent = userAgent;
    }

    public String getIp() {
        return ip;
    }

    public String getTime() {
        return time;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getBodyBytesSent() {
        return bodyBytesSent;
    }

    public String getReferer() {
        return referer;
    }

    public String getUserAgent() {
        return userAgent;
    }
}