package ca.corbett.ems.app.ui;

import ca.corbett.ems.app.Version;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.NumberField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.TextField;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A JPanel that contains controls for starting a local EMS server or for
 * connecting to a remote one. Also contains a server output console
 * for visibility as to what's going on within the server.
 */
public final class ServerPanel extends JPanel {

    private ComboField sourceField;
    private TextField hostField;
    private NumberField portField;
    private JTextArea textArea;

    public ServerPanel() {
        setLayout(new BorderLayout());
        add(buildControlPanel(), BorderLayout.NORTH);
        add(buildConsole(), BorderLayout.CENTER);
    }

    public void clearConsole() {
        textArea.setText("");
    }

    public void appendToConsole(String text) {
        textArea.append(text + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    private JPanel buildControlPanel() {
        FormPanel formPanel = new FormPanel(FormPanel.Alignment.TOP_LEFT);

        List<String> options = new ArrayList<>();
        options.add("Start a local EMS server");
        options.add("Connect to a remote EMS server");
        sourceField = new ComboField("Source:", options, 0, false);
        formPanel.addFormField(sourceField);

        hostField = new TextField("Host:", 15, 1, true);
        hostField.setText("localhost");
        formPanel.addFormField(hostField);

        portField = new NumberField("Port:", 1975, 1024, 65535, 1);
        formPanel.addFormField(portField);

        PanelField panelField = new PanelField();
        panelField.getPanel().setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton btn = new JButton("Connect");
        btn.setPreferredSize(new Dimension(120, 25));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sourceField.getSelectedIndex() == 0) {
                    appendToConsole("Starting local EMS server...");
                    MainWindow.getInstance().startLocalServer(hostField.getText(), (Integer) portField.getCurrentValue());
                }
                appendToConsole("Attempting to connect...");
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(75); // give it a chance to start up
                        } catch (InterruptedException ignored) {
                        }
                        MainWindow.getInstance().connect(hostField.getText(), (Integer) portField.getCurrentValue());
                    }
                });
            }
        });
        panelField.getPanel().add(btn);
        btn = new JButton("Disconnect");
        btn.setPreferredSize(new Dimension(120, 25));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.getInstance().disconnect();
            }
        });
        panelField.getPanel().add(btn);
        formPanel.addFormField(panelField);

        formPanel.render();
        return formPanel;
    }

    private JPanel buildConsole() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setForeground(Color.GREEN);
        textArea.setBackground(Color.BLACK);
        textArea.setEditable(false);
        textArea.setRows(8);
        textArea.setText(Version.FULL_NAME + "\n" + Version.PROJECT_URL + "\n" + "Ready.\n");
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.getVerticalScrollBar().setBlockIncrement(64);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
}
