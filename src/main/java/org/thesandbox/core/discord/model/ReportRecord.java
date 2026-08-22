package org.thesandbox.core.discord.model;

public class ReportRecord {
public final String reporter, target, reason;
        public final int x, y, z;
        public String messageId;
        public ReportRecord(String reporter, String target, String reason, int x, int y, int z) {
            this.reporter = reporter; this.target = target; this.reason = reason;
            this.x = x; this.y = y; this.z = z;
        }
}
