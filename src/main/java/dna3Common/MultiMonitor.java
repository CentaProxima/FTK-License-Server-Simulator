package dna3Common;

import java.util.Hashtable;

public class MultiMonitor {
    private int maxMonitors;

    private Hashtable owners = new Hashtable<>();

    protected class MonitorOwner {
        Thread thread;

        int count;

        public MonitorOwner(Thread t) {
            this.thread = t;
            this.count = 0;
        }
    }

    public MultiMonitor(int maxM) {
        if (maxM < 1)
            maxM = 1;
        this.maxMonitors = maxM;
    }

    public void acquireMonitor() {
        boolean acquired = false;
        Thread thread = Thread.currentThread();
        while (!acquired) {
            Thread.yield();
            synchronized (this) {
                MonitorOwner mo = (MonitorOwner)this.owners.get(thread);
                if (mo != null) {
                    mo.count++;
                    acquired = true;
                } else if (this.owners.size() < this.maxMonitors) {
                    mo = new MonitorOwner(thread);
                    mo.count++;
                    this.owners.put(thread, mo);
                    acquired = true;
                }
            }
        }
    }

    public synchronized void releaseMonitor() {
        Thread thread = Thread.currentThread();
        MonitorOwner mo = (MonitorOwner)this.owners.get(thread);
        if (mo != null) {
            mo.count--;
            if (mo.count <= 0)
                this.owners.remove(thread);
        }
    }

    public synchronized void reset() {
        this.owners.clear();
    }

    public synchronized void resetThread() {
        Thread thread = Thread.currentThread();
        this.owners.remove(thread);
    }
}
