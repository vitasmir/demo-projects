package cz.miro.passwordvault;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import cz.miro.passwordvault.service.CryptoUtils;
import cz.miro.passwordvault.service.VaultRepository;
import cz.miro.passwordvault.service.VaultService;
import cz.miro.passwordvault.ui.PasswordVaultFrame;

public final class PasswordVaultApp {
    private PasswordVaultApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            increaseApplicationFontSize();

            Path vaultPath = Path.of(System.getProperty("user.home"), ".password-vault", "vault.dat");
            VaultService service = new VaultService(new VaultRepository(vaultPath));
            openApplication(service, vaultPath);
        });
    }

    private static void openApplication(VaultService service, Path vaultPath) {
        try {
            VaultService.VaultSession session = service.vaultExists()
                    ? unlockExistingVault(service)
                    : createNewVault(service);

            if (session == null) {
                return;
            }

            PasswordVaultFrame frame = new PasswordVaultFrame(session, vaultPath);
            frame.setVisible(true);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Chyba", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static VaultService.VaultSession createNewVault(VaultService service) {
        while (true) {
            JPasswordField passwordField = new JPasswordField();
            JPasswordField confirmField = new JPasswordField();
            int result = showPasswordDialog(
                createPasswordPanel(
                    "Trezor neexistuje. Nastavte nové hlavní heslo.",
                    passwordField,
                    "Hlavní heslo:",
                    confirmField,
                    "Potvrzení hesla:"
                ),
                passwordField,
                "Vytvořit trezor"
            );

            if (result != JOptionPane.OK_OPTION) {
                CryptoUtils.wipe(passwordField.getPassword());
                CryptoUtils.wipe(confirmField.getPassword());
                return null;
            }

            char[] password = passwordField.getPassword();
            char[] confirmation = confirmField.getPassword();
            try {
                validateNewPassword(password, confirmation);
                return service.createVault(password);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Neplatné heslo", JOptionPane.WARNING_MESSAGE);
            } finally {
                CryptoUtils.wipe(password);
                CryptoUtils.wipe(confirmation);
            }
        }
    }

    private static VaultService.VaultSession unlockExistingVault(VaultService service) {
        while (true) {
            JPasswordField passwordField = new JPasswordField();
            int result = showPasswordDialog(
                createSinglePasswordPanel("Zadejte hlavní heslo pro odemčení trezoru.", passwordField, "Hlavní heslo:"),
                passwordField,
                "Odemknout trezor"
            );

            if (result != JOptionPane.OK_OPTION) {
                CryptoUtils.wipe(passwordField.getPassword());
                return null;
            }

            char[] password = passwordField.getPassword();
            try {
                return service.openVault(password);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Neplatné heslo", JOptionPane.ERROR_MESSAGE);
            } finally {
                CryptoUtils.wipe(password);
            }
        }
    }

    private static void validateNewPassword(char[] password, char[] confirmation) {
        if (password.length < 12) {
            throw new IllegalArgumentException("Hlavní heslo musí mít alespoň 12 znaků.");
        }
        if (!Arrays.equals(password, confirmation)) {
            throw new IllegalArgumentException("Hlavní hesla se neshodují.");
        }
    }

    private static JComponent createSinglePasswordPanel(String message, JPasswordField passwordField, String passwordLabel) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(message));
        panel.add(new JLabel(" "));
        panel.add(new JLabel(passwordLabel));
        panel.add(passwordField);
        return panel;
    }

    private static JComponent createPasswordPanel(
            String message,
            JPasswordField passwordField,
            String passwordLabel,
            JPasswordField confirmationField,
            String confirmationLabel
    ) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(message));
        panel.add(new JLabel(" "));
        panel.add(new JLabel(passwordLabel));
        panel.add(passwordField);
        panel.add(new JLabel(" "));
        panel.add(new JLabel(confirmationLabel));
        panel.add(confirmationField);
        return panel;
    }

    private static int showPasswordDialog(JComponent panel, JPasswordField passwordField, String title) {
        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(null, title);
        dialog.setModal(true);
        dialog.setAutoRequestFocus(true);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                SwingUtilities.invokeLater(() -> {
                    dialog.toFront();
                    dialog.requestFocus();
                    passwordField.requestFocusInWindow();
                });
            }
        });
        dialog.setVisible(true);

        Object selectedValue = optionPane.getValue();
        dialog.dispose();
        return selectedValue instanceof Integer ? (Integer) selectedValue : JOptionPane.CLOSED_OPTION;
    }

    private static void increaseApplicationFontSize() {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource font) {
                float newSize = Math.max(18.0f, font.getSize2D() + 5.0f);
                UIManager.put(key, new FontUIResource(font.deriveFont(Font.PLAIN, newSize)));
            }
        }
    }
}
