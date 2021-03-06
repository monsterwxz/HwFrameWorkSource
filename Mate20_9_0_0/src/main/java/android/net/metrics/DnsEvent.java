package android.net.metrics;

import android.net.NetworkCapabilities;
import com.android.internal.util.BitUtils;
import java.util.Arrays;

public final class DnsEvent {
    private static final int SIZE_LIMIT = 20000;
    public int eventCount;
    public byte[] eventTypes;
    public int[] latenciesMs;
    public final int netId;
    public byte[] returnCodes;
    public int successCount;
    public final long transports;

    public DnsEvent(int netId, long transports, int initialCapacity) {
        this.netId = netId;
        this.transports = transports;
        this.eventTypes = new byte[initialCapacity];
        this.returnCodes = new byte[initialCapacity];
        this.latenciesMs = new int[initialCapacity];
    }

    boolean addResult(byte eventType, byte returnCode, int latencyMs) {
        boolean isSuccess = returnCode == (byte) 0;
        if (this.eventCount >= 20000) {
            return isSuccess;
        }
        if (this.eventCount == this.eventTypes.length) {
            resize((int) (1.4d * ((double) this.eventCount)));
        }
        this.eventTypes[this.eventCount] = eventType;
        this.returnCodes[this.eventCount] = returnCode;
        this.latenciesMs[this.eventCount] = latencyMs;
        this.eventCount++;
        if (isSuccess) {
            this.successCount++;
        }
        return isSuccess;
    }

    public void resize(int newLength) {
        this.eventTypes = Arrays.copyOf(this.eventTypes, newLength);
        this.returnCodes = Arrays.copyOf(this.returnCodes, newLength);
        this.latenciesMs = Arrays.copyOf(this.latenciesMs, newLength);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("DnsEvent(");
        builder.append("netId=");
        builder.append(this.netId);
        builder = builder.append(", ");
        for (int t : BitUtils.unpackBits(this.transports)) {
            builder.append(NetworkCapabilities.transportNameOf(t));
            builder.append(", ");
        }
        builder.append(String.format("%d events, ", new Object[]{Integer.valueOf(this.eventCount)}));
        builder.append(String.format("%d success)", new Object[]{Integer.valueOf(this.successCount)}));
        return builder.toString();
    }
}
