package dna3;

import java.awt.AWTEvent;

public class InfoEvent extends AWTEvent {
    private String info;

    public InfoEvent(Object source, int id, String info) {
        super(source, id);
        this.info = info;
    }

    public String getInfo() {
        return this.info;
    }
}
