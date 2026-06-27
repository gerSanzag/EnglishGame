package com.englishgame;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Modal dialog to choose the learning mode when the app starts (single JAR).
 */
public final class GameModeSelector {

    private GameModeSelector() {
    }

    /**
     * @return chosen mode, or null if the user closed the dialog without choosing
     */
    public static AppGameMode show() {
        if (!SwingUtilities.isEventDispatchThread()) {
            final AppGameMode[] holder = new AppGameMode[1];
            try {
                SwingUtilities.invokeAndWait(() -> holder[0] = showOnEdt());
            } catch (Exception e) {
                return null;
            }
            return holder[0];
        }
        return showOnEdt();
    }

    private static AppGameMode showOnEdt() {
        JDialog dialog = new JDialog((java.awt.Frame) null, "English Learning Game", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(12, 12));

        JLabel title = new JLabel("Choose learning mode", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

        JLabel hint = new JLabel(
                "<html><center>Each mode uses its own data file.<br>"
                        + "You can choose again when you restart the application.</center></html>",
                JLabel.CENTER);
        hint.setFont(new Font("Arial", Font.PLAIN, 13));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));

        final AppGameMode[] choice = new AppGameMode[1];

        JPanel buttons = new JPanel(new GridLayout(2, 1, 10, 10));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));

        JButton classicBtn = modeButton(AppGameMode.CLASSIC);
        classicBtn.addActionListener(e -> {
            choice[0] = AppGameMode.CLASSIC;
            dialog.dispose();
        });

        JButton definitionBtn = modeButton(AppGameMode.DEFINITION);
        definitionBtn.addActionListener(e -> {
            choice[0] = AppGameMode.DEFINITION;
            dialog.dispose();
        });

        buttons.add(classicBtn);
        buttons.add(definitionBtn);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancel = new JButton("Exit");
        cancel.addActionListener(e -> dialog.dispose());
        south.add(cancel);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.add(hint, BorderLayout.NORTH);
        center.add(buttons, BorderLayout.CENTER);

        dialog.add(title, BorderLayout.NORTH);
        dialog.add(center, BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(520, 280));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return choice[0];
    }

    private static JButton modeButton(AppGameMode mode) {
        JButton button = new JButton("<html><center><b>" + mode.getSelectorLabel() + "</b></center></html>");
        button.setFont(new Font("Arial", Font.PLAIN, 15));
        button.setPreferredSize(new Dimension(420, 56));
        return button;
    }
}
