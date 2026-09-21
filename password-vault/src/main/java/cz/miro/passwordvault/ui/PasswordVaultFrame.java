package cz.miro.passwordvault.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import cz.miro.passwordvault.service.VaultService;

public final class PasswordVaultFrame extends JFrame {
    private final VaultService.VaultSession session;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField websiteField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel statusLabel;
    private JCheckBox showPassword;
    private String selectedWebsite;
    private String loadedPassword;
    private char defaultEchoChar;

    public PasswordVaultFrame(VaultService.VaultSession session, Path vaultPath) {
        super("Password Vault");
        this.session = session;
        this.tableModel = new DefaultTableModel(new Object[]{"Web", "Uživatelské jméno"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.table = new JTable(tableModel);
        this.websiteField = new JTextField(24);
        this.usernameField = new JTextField(24);
        this.passwordField = new JPasswordField(24);
        this.statusLabel = new JLabel("Trezor připraven", SwingConstants.LEFT);
        this.loadedPassword = "";

        initializeUi(vaultPath);
        reloadTable(null);
    }

    private void initializeUi(Path vaultPath) {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1600, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        JLabel topLabel = new JLabel("Soubor trezoru: " + vaultPath);
        topLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        add(topLabel, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(30);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedRow();
            }
        });

        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(BorderFactory.createTitledBorder("Uložené přístupy"));
        tablePane.setMinimumSize(new Dimension(520, 0));

        JPanel editorPanel = buildEditorPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePane, editorPanel);
        splitPane.setResizeWeight(0.5);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel buildEditorPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Detail záznamu"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Web:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(websiteField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Uživatelské jméno:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Heslo:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(passwordField, gbc);
        defaultEchoChar = passwordField.getEchoChar();

        gbc.gridx = 1;
        gbc.gridy = 3;
        showPassword = new JCheckBox("Zobrazit psané heslo");
        showPassword.addActionListener(event -> passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : defaultEchoChar));
        formPanel.add(showPassword, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton newButton = new JButton("Nový");
        JButton saveButton = new JButton("Uložit");
        JButton deleteButton = new JButton("Smazat");
        JButton clearButton = new JButton("Vyčistit");
        JButton copyUsernameButton = new JButton("Kopírovat jméno");
        JButton copyPasswordButton = new JButton("Kopírovat heslo");

        newButton.addActionListener(event -> clearForm(true));
        saveButton.addActionListener(event -> saveEntry());
        deleteButton.addActionListener(event -> deleteEntry());
        clearButton.addActionListener(event -> clearForm(false));
        copyUsernameButton.addActionListener(event -> copyUsername());
        copyPasswordButton.addActionListener(event -> copyPassword());

        buttonPanel.add(newButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(copyUsernameButton);
        buttonPanel.add(copyPasswordButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(buttonPanel, gbc);

        return formPanel;
    }

    private void loadSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        String website = String.valueOf(tableModel.getValueAt(selectedRow, 0));
        session.findByWebsite(website).ifPresent(credential -> {
            selectedWebsite = credential.website();
            loadedPassword = credential.password();
            websiteField.setText(credential.website());
            usernameField.setText(credential.username());
            resetShowPassword();
            passwordField.setText(loadedPassword);
            passwordField.setEchoChar(defaultEchoChar);
            setStatus("Načten záznam pro web " + credential.website());
        });
    }

    private void saveEntry() {
        String website = websiteField.getText().trim();
        String username = usernameField.getText().trim();
        String typedPassword = new String(passwordField.getPassword());
        String password = typedPassword.isEmpty() ? loadedPassword : typedPassword;

        if (website.isBlank()) {
            JOptionPane.showMessageDialog(this, "Web nesmí být prázdný.", "Neplatná data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Heslo nesmí být prázdné.", "Neplatná data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedWebsite != null && !normalize(selectedWebsite).equals(normalize(website))) {
            session.remove(selectedWebsite);
        }

        session.addOrUpdate(website, username, password);
        loadedPassword = password;
        passwordField.setText("");
        reloadTable(website);
        setStatus("Záznam byl uložen.");
    }

    private void deleteEntry() {
        String website = websiteField.getText().trim();
        if (website.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vyberte nebo zadejte záznam ke smazání.", "Nic ke smazání", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this, "Opravdu smazat záznam pro web " + website + "?", "Potvrzení smazání", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        boolean removed = session.remove(website);
        if (removed) {
            clearForm(true);
            reloadTable(null);
            setStatus("Záznam byl smazán.");
        } else {
            JOptionPane.showMessageDialog(this, "Záznam nebyl nalezen.", "Nenalezeno", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void reloadTable(String websiteToSelect) {
        List<VaultService.DecryptedCredential> entries = session.listEntries();
        tableModel.setRowCount(0);
        for (VaultService.DecryptedCredential entry : entries) {
            tableModel.addRow(new Object[]{entry.website(), entry.username()});
        }

        if (websiteToSelect != null) {
            selectWebsite(websiteToSelect);
        }
    }

    private void selectWebsite(String website) {
        String normalized = normalize(website);
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String rowWebsite = String.valueOf(tableModel.getValueAt(row, 0));
            if (normalize(rowWebsite).equals(normalized)) {
                table.setRowSelectionInterval(row, row);
                table.scrollRectToVisible(table.getCellRect(row, 0, true));
                resetShowPassword();
                return;
            }
        }
    }

    private void clearForm(boolean clearTableSelection) {
        selectedWebsite = null;
        loadedPassword = "";
        websiteField.setText("");
        usernameField.setText("");
        resetShowPassword();
        passwordField.setText("");
        if (clearTableSelection) {
            table.clearSelection();
        }
        websiteField.requestFocusInWindow();
        setStatus("Formulář je připraven.");
    }

    private void resetShowPassword() {
        if (showPassword != null) {
            showPassword.setSelected(false);
            passwordField.setEchoChar(defaultEchoChar);
            showPassword.repaint();
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void copyUsername() {
        String username = usernameField.getText();
        if (username.isBlank()) {
            JOptionPane.showMessageDialog(this, "Není co kopírovat.", "Prázdné jméno", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        copyToClipboard(username);
        setStatus("Uživatelské jméno bylo zkopírováno do schránky.");
    }

    private void copyPassword() {
        String typedPassword = new String(passwordField.getPassword());
        String password = typedPassword.isEmpty() ? loadedPassword : typedPassword;
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Není co kopírovat.", "Prázdné heslo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        copyToClipboard(password);
        setStatus("Heslo bylo zkopírováno do schránky.");
    }

    private static void copyToClipboard(String value) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
    }

    private static String normalize(String website) {
        return website == null ? "" : website.trim().toLowerCase();
    }
}
