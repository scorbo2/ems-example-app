package ca.corbett.ems.app.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Represents a JPanel in the very bottom of the MainWindow that
 * can relay basic information to the user about current connection status.
 *
 * @author scorbo2
 * @since 2025-03-18
 */
public final class StatusBar extends JPanel {

    private final JLabel statusLabel;

    public StatusBar() {
        setPreferredSize(new Dimension(1, 32));
        statusLabel = new JLabel("Not connected.");
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(statusLabel);
        setBorder(BorderFactory.createLoweredBevelBorder());
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }
}
