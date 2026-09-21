package cz.miro.passwordvault.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import javax.swing.TransferHandler;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import cz.miro.passwordvault.service.CryptoUtils;
import cz.miro.passwordvault.service.VaultService;

public final class PasswordVaultFrame extends JFrame {
    private final VaultService.VaultSession session;
    private final Path vaultPath;
    private final CredentialTableModel tableModel;
    private final JTable table;
    private final DefaultComboBoxModel<String> groupComboModel;
    private final JComboBox<String> groupCombo;
    private final JTextField serverNameField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel statusLabel;
    private final JLabel currentGroupLabel;
    private final DefaultMutableTreeNode treeRoot;
    private final DefaultTreeModel treeModel;
    private final javax.swing.JTree groupTree;
    private boolean refreshingViews;
    private String selectedGroup;
    private String loadedGroup;
    private String loadedServerName;
    private String loadedPassword;

    public PasswordVaultFrame(VaultService.VaultSession session, Path vaultPath) {
        super("PasswordManager");
        this.session = session;
        this.vaultPath = vaultPath;
        this.tableModel = new CredentialTableModel();
        this.table = new JTable(tableModel);
        this.groupComboModel = new DefaultComboBoxModel<>();
        this.groupCombo = new JComboBox<>(groupComboModel);
        this.serverNameField = new JTextField(28);
        this.usernameField = new JTextField(28);
        this.passwordField = new JPasswordField(28);
        this.statusLabel = new JLabel("Trezor je odemčen", SwingConstants.LEFT);
        this.currentGroupLabel = new JLabel();
        this.treeRoot = new DefaultMutableTreeNode("Skupiny");
        this.treeModel = new DefaultTreeModel(treeRoot);
        this.groupTree = new javax.swing.JTree(treeModel);
        this.loadedPassword = "";

        this.groupCombo.setEditable(false);
        this.groupCombo.setEnabled(false);
        this.passwordField.setEchoChar('*');

        initializeUi();
        refreshViews(null, null);
    }

    private void initializeUi() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1500, 860));
        setSize(1680, 920);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        add(buildTopBar(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildGroupPanel(), buildRightPanel());
        splitPane.setResizeWeight(0.22);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.22));
        add(splitPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        footer.add(statusLabel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private JComponent buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

        JLabel title = new JLabel("PasswordManager");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel vaultLocation = new JLabel("Soubor trezoru: " + vaultPath);
        vaultLocation.setHorizontalAlignment(SwingConstants.RIGHT);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(vaultLocation, BorderLayout.EAST);
        return topBar;
    }

    private JComponent buildGroupPanel() {
        JPanel groupPanel = new JPanel(new BorderLayout(8, 8));
        groupPanel.setBorder(BorderFactory.createTitledBorder("Skupiny"));

        JPanel groupToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton addGroupButton = new JButton("+");
        JButton removeGroupButton = new JButton("-");

        addGroupButton.setToolTipText("Vytvořit novou podskupinu pod vybraným uzlem");
        removeGroupButton.setToolTipText("Smazat vybranou skupinu i podskupiny");

        addGroupButton.addActionListener(event -> createGroup());
        removeGroupButton.addActionListener(event -> deleteSelectedGroup());
        JButton renameGroupButton = new JButton("Přejmenovat");
        renameGroupButton.setToolTipText("Přejmenovat vybranou skupinu");
        renameGroupButton.addActionListener(event -> renameSelectedGroup());

        groupToolbar.add(addGroupButton);
        groupToolbar.add(renameGroupButton);
        groupToolbar.add(removeGroupButton);

        groupTree.setRootVisible(false);
        groupTree.setShowsRootHandles(true);
        groupTree.setDragEnabled(true);
        groupTree.setDropMode(DropMode.ON);
        groupTree.setTransferHandler(new GroupTreeTransferHandler());
        groupTree.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
        groupTree.addTreeSelectionListener(event -> {
            if (refreshingViews) {
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) groupTree.getLastSelectedPathComponent();
            if (node == null || node == treeRoot) {
                return;
            }

            Object nodeValue = node.getUserObject();
            if (nodeValue instanceof GroupNode groupNode) {
                loadGroup(groupNode.path());
            }
        });

        JScrollPane treeScrollPane = new JScrollPane(groupTree);
        treeScrollPane.setPreferredSize(new Dimension(280, 0));
        groupPanel.add(groupToolbar, BorderLayout.NORTH);
        groupPanel.add(treeScrollPane, BorderLayout.CENTER);

        return groupPanel;
    }

    private JComponent buildRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));

        rightPanel.add(buildTableHeader(), BorderLayout.NORTH);
        rightPanel.add(buildTablePane(), BorderLayout.CENTER);
        rightPanel.add(buildEditorPanel(), BorderLayout.SOUTH);

        return rightPanel;
    }

    private JComponent buildTableHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        currentGroupLabel.setFont(currentGroupLabel.getFont().deriveFont(Font.BOLD, 15f));
        header.add(currentGroupLabel, BorderLayout.WEST);

        JPanel copyButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton copyServerButton = new JButton("Kopírovat server name");
        JButton copyUsernameButton = new JButton("Kopírovat username");
        JButton copyPasswordButton = new JButton("Kopírovat password");

        copyServerButton.addActionListener(event -> copyValue(ValueKind.SERVER_NAME));
        copyUsernameButton.addActionListener(event -> copyValue(ValueKind.USERNAME));
        copyPasswordButton.addActionListener(event -> copyValue(ValueKind.PASSWORD));

        copyButtons.add(copyServerButton);
        copyButtons.add(copyUsernameButton);
        copyButtons.add(copyPasswordButton);

        header.add(copyButtons, BorderLayout.EAST);
        return header;
    }

    private JComponent buildTablePane() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !refreshingViews) {
                loadSelectedRow();
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(280);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);

        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Přístupy ve vybrané skupině"));
        return tableScrollPane;
    }

    private JComponent buildEditorPanel() {
        JPanel editorPanel = new JPanel(new GridBagLayout());
        editorPanel.setBorder(BorderFactory.createTitledBorder("Detail záznamu"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        editorPanel.add(new JLabel("Skupina:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        editorPanel.add(groupCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        editorPanel.add(new JLabel("Server name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        editorPanel.add(serverNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        editorPanel.add(new JLabel("username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        editorPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        editorPanel.add(new JLabel("password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        editorPanel.add(passwordField, gbc);

        JLabel hint = new JLabel("Hesla jsou v tabulce skryta pomocí *.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, hint.getFont().getSize2D() - 1f));
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        editorPanel.add(hint, gbc);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton saveButton = new JButton("Uložit změny");
        JButton deleteButton = new JButton("Smazat");
        JButton clearButton = new JButton("Vymazat formulář");

        saveButton.addActionListener(event -> saveEntry());
        deleteButton.addActionListener(event -> deleteEntry());
        clearButton.addActionListener(event -> clearForm(true));

        buttonRow.add(saveButton);
        buttonRow.add(deleteButton);
        buttonRow.add(clearButton);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        editorPanel.add(buttonRow, gbc);

        return editorPanel;
    }

    private void loadGroup(String groupName) {
        refreshViews(groupName, null);
        setStatus("Vybrána skupina: " + groupName);
    }

    private void createGroup() {
        String parentGroup = getSelectedTreeGroupPath();
        String groupName = JOptionPane.showInputDialog(this, "Zadejte název nové skupiny:", "Nová skupina", JOptionPane.PLAIN_MESSAGE);
        if (groupName == null) {
            return;
        }

        groupName = groupName.trim();
        if (groupName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Název skupiny nesmí být prázdný.", "Neplatný název", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!session.addGroup(parentGroup, groupName)) {
            JOptionPane.showMessageDialog(this, "Skupina už existuje.", "Duplicitní skupina", JOptionPane.INFORMATION_MESSAGE);
        }

        String createdGroupPath = normalizeGroupPath(parentGroup == null || parentGroup.isBlank() ? groupName : parentGroup + "/" + groupName);
        refreshViews(createdGroupPath, null);
        selectGroupInTree(createdGroupPath);
        setStatus("Skupina byla vytvořena.");
    }

    private void renameSelectedGroup() {
        String groupPath = getSelectedTreeGroupPath();
        if (groupPath == null || groupPath.isBlank()) {
            JOptionPane.showMessageDialog(this, "Nejprve vyberte skupinu.", "Nic k přejmenování", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (VaultService.DEFAULT_GROUP.equalsIgnoreCase(groupPath)) {
            JOptionPane.showMessageDialog(this, "Výchozí skupinu nelze přejmenovat.", "Nelze přejmenovat", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String currentName = leafName(groupPath);
        String newName = JOptionPane.showInputDialog(this, "Zadejte nový název skupiny:", currentName);
        if (newName == null) {
            return;
        }

        newName = newName.trim();
        if (newName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Název skupiny nesmí být prázdný.", "Neplatný název", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!session.renameGroup(groupPath, newName)) {
            JOptionPane.showMessageDialog(this, "Skupinu se nepodařilo přejmenovat.", "Chyba", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String renamedGroupPath = parentPath(groupPath).isBlank() ? normalizeGroupPath(newName) : parentPath(groupPath) + "/" + normalizeGroupPath(newName);
        refreshViews(renamedGroupPath, null);
        selectGroupInTree(renamedGroupPath);
        setStatus("Skupina byla přejmenována.");
    }

    private void deleteSelectedGroup() {
        String groupName = getSelectedTreeGroupPath();
        if (groupName == null || groupName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Nejprve vyberte skupinu.", "Nic ke smazání", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (VaultService.DEFAULT_GROUP.equalsIgnoreCase(groupName)) {
            JOptionPane.showMessageDialog(this, "Výchozí skupinu nelze smazat.", "Nelze smazat", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Opravdu smazat skupinu " + groupName + " a všechny její záznamy?",
                "Potvrzení smazání",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        if (!session.removeGroup(groupName)) {
            JOptionPane.showMessageDialog(this, "Skupinu se nepodařilo smazat.", "Chyba", JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshViews(VaultService.DEFAULT_GROUP, null);
        selectGroupInTree(VaultService.DEFAULT_GROUP);
        setStatus("Skupina byla smazána.");
    }

    private void refreshViews(String groupToSelect, String serverToSelect) {
        refreshingViews = true;
        try {
            List<String> groups = new ArrayList<>(session.listGroups());
            if (groups.isEmpty()) {
                groups.add(VaultService.DEFAULT_GROUP);
            }

            updateGroupModels(groups);
            rebuildGroupTree(groups);

            String resolvedGroup = resolveGroupSelection(groupToSelect, groups);
            selectedGroup = resolvedGroup;
            currentGroupLabel.setText("Aktuální skupina: " + resolvedGroup);
            groupCombo.setSelectedItem(resolvedGroup);

            tableModel.setEntries(session.listEntries(resolvedGroup));
            expandAndSelectGroup(resolvedGroup);
        } finally {
            refreshingViews = false;
        }

        if (serverToSelect != null) {
            selectRowByServerName(serverToSelect);
        } else {
            table.clearSelection();
            clearForm(false);
        }
    }

    private void updateGroupModels(List<String> groups) {
        groupComboModel.removeAllElements();

        for (String groupName : groups) {
            groupComboModel.addElement(groupName);
        }
    }

    private void rebuildGroupTree(List<String> groups) {
        treeRoot.removeAllChildren();
        for (String groupName : groups) {
            addGroupNode(treeRoot, normalizeGroupPath(groupName));
        }
        treeModel.reload();
    }

    private void addGroupNode(DefaultMutableTreeNode rootNode, String groupPath) {
        String[] segments = normalizeGroupPath(groupPath).split("/");
        DefaultMutableTreeNode currentNode = rootNode;
        StringBuilder currentPath = new StringBuilder();

        for (String segment : segments) {
            if (segment.isBlank()) {
                continue;
            }

            if (currentPath.length() > 0) {
                currentPath.append('/');
            }
            currentPath.append(segment);

            DefaultMutableTreeNode childNode = findChildNode(currentNode, currentPath.toString());
            if (childNode == null) {
                childNode = new DefaultMutableTreeNode(new GroupNode(segment, currentPath.toString()));
                currentNode.add(childNode);
            }
            currentNode = childNode;
        }
    }

    private DefaultMutableTreeNode findChildNode(DefaultMutableTreeNode parentNode, String groupPath) {
        for (int index = 0; index < parentNode.getChildCount(); index++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(index);
            Object userObject = childNode.getUserObject();
            if (userObject instanceof GroupNode groupNode && groupNode.path().equalsIgnoreCase(groupPath)) {
                return childNode;
            }
        }
        return null;
    }

    private String resolveGroupSelection(String requestedGroup, List<String> groups) {
        if (requestedGroup != null) {
            for (String groupName : groups) {
                if (groupName.equalsIgnoreCase(requestedGroup.trim())) {
                    return groupName;
                }
            }
        }
        return groups.get(0);
    }

    private void expandAndSelectGroup(String groupName) {
        DefaultMutableTreeNode groupNode = findGroupNode(groupName);
        if (groupNode == null) {
            return;
        }

        TreePath treePath = new TreePath(groupNode.getPath());
        groupTree.setSelectionPath(treePath);
        groupTree.scrollPathToVisible(treePath);
    }

    private void selectGroupInTree(String groupName) {
        DefaultMutableTreeNode groupNode = findGroupNode(groupName);
        if (groupNode == null) {
            return;
        }

        TreePath treePath = new TreePath(groupNode.getPath());
        groupTree.setSelectionPath(treePath);
        groupTree.scrollPathToVisible(treePath);
    }

    private DefaultMutableTreeNode findGroupNode(String groupName) {
        String normalizedGroupName = normalizeGroupPath(groupName);
        for (int index = 0; index < treeRoot.getChildCount(); index++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeRoot.getChildAt(index);
            if (child.getUserObject() instanceof GroupNode groupNode && groupNode.path().equalsIgnoreCase(normalizedGroupName)) {
                return child;
            }

            DefaultMutableTreeNode nestedMatch = findGroupNode(child, normalizedGroupName);
            if (nestedMatch != null) {
                return nestedMatch;
            }
        }
        return null;
    }

    private DefaultMutableTreeNode findGroupNode(DefaultMutableTreeNode parentNode, String groupName) {
        for (int index = 0; index < parentNode.getChildCount(); index++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parentNode.getChildAt(index);
            if (child.getUserObject() instanceof GroupNode groupNode && groupNode.path().equalsIgnoreCase(groupName)) {
                return child;
            }

            DefaultMutableTreeNode nestedMatch = findGroupNode(child, groupName);
            if (nestedMatch != null) {
                return nestedMatch;
            }
        }
        return null;
    }

    private void loadSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        VaultService.DecryptedCredential credential = tableModel.getEntryAt(selectedRow);
        selectedGroup = credential.groupName();
        loadedGroup = credential.groupName();
        loadedServerName = credential.serverName();
        loadedPassword = credential.password();

        groupCombo.setSelectedItem(credential.groupName());
        serverNameField.setText(credential.serverName());
        usernameField.setText(credential.username());
        passwordField.setText(credential.password());
        currentGroupLabel.setText("Aktuální skupina: " + credential.groupName());
        setStatus("Načten záznam pro server " + credential.serverName());
    }

    private void selectRowByServerName(String serverName) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            VaultService.DecryptedCredential credential = tableModel.getEntryAt(row);
            if (credential.serverName().equalsIgnoreCase(serverName)) {
                table.setRowSelectionInterval(row, row);
                table.scrollRectToVisible(table.getCellRect(row, 0, true));
                return;
            }
        }

        table.clearSelection();
    }

    private void saveEntry() {
        String groupName = readGroupName();
        String serverName = serverNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = readPasswordField();

        if (serverName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Server name nesmí být prázdný.", "Neplatná data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.isBlank()) {
            JOptionPane.showMessageDialog(this, "Password nesmí být prázdné.", "Neplatná data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (loadedGroup != null && loadedServerName != null
                && (!loadedGroup.equalsIgnoreCase(groupName) || !loadedServerName.equalsIgnoreCase(serverName))) {
            session.remove(loadedGroup, loadedServerName);
        }

        session.addOrUpdate(groupName, serverName, username, password);
        loadedGroup = groupName;
        loadedServerName = serverName;
        loadedPassword = password;

        refreshViews(groupName, serverName);
        setStatus("Záznam byl uložen.");
    }

    private void deleteEntry() {
        String groupName = readGroupName();
        String serverName = serverNameField.getText().trim();

        if (serverName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vyberte nebo vyplňte záznam ke smazání.", "Nic ke smazání", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Opravdu smazat záznam pro server " + serverName + " ve skupině " + groupName + "?",
                "Potvrzení smazání",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        boolean removed = session.remove(groupName, serverName);
        if (removed) {
            clearForm(true);
            refreshViews(groupName, null);
            setStatus("Záznam byl smazán.");
        } else {
            JOptionPane.showMessageDialog(this, "Záznam nebyl nalezen.", "Nenalezeno", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearForm(boolean clearTableSelection) {
        if (clearTableSelection) {
            table.clearSelection();
        }

        loadedGroup = null;
        loadedServerName = null;
        loadedPassword = "";

        String groupName = selectedGroup == null || selectedGroup.isBlank() ? VaultService.DEFAULT_GROUP : selectedGroup;
        groupCombo.setSelectedItem(groupName);
        serverNameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        setStatus("Formulář je připraven.");
    }

    private void copyValue(ValueKind valueKind) {
        VaultService.DecryptedCredential credential = getSelectedCredential();
        if (credential == null) {
            JOptionPane.showMessageDialog(this, "Nejprve vyberte řádek v tabulce.", "Nic ke kopírování", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String value = switch (valueKind) {
            case SERVER_NAME -> credential.serverName();
            case USERNAME -> credential.username();
            case PASSWORD -> credential.password();
        };

        if (value == null || value.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vybraný údaj je prázdný.", "Nic ke kopírování", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        copyToClipboard(value);
        setStatus(switch (valueKind) {
            case SERVER_NAME -> "Server name bylo zkopírováno do schránky.";
            case USERNAME -> "Username bylo zkopírováno do schránky.";
            case PASSWORD -> "Password bylo zkopírováno do schránky.";
        });
    }

    private VaultService.DecryptedCredential getSelectedCredential() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            return tableModel.getEntryAt(selectedRow);
        }

        String groupName = readGroupName();
        String serverName = serverNameField.getText().trim();
        if (groupName.isBlank() || serverName.isBlank()) {
            return null;
        }

        return session.findByGroupAndServer(groupName, serverName).orElse(null);
    }

    private String readGroupName() {
        Object editorValue = groupCombo.isEditable() ? groupCombo.getEditor().getItem() : groupCombo.getSelectedItem();
        String groupName = editorValue == null ? "" : editorValue.toString().trim();
        if (groupName.isBlank()) {
            groupName = selectedGroup == null || selectedGroup.isBlank() ? VaultService.DEFAULT_GROUP : selectedGroup;
        }
        return groupName;
    }

    private String getSelectedTreeGroupPath() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) groupTree.getLastSelectedPathComponent();
        if (node == null) {
            return selectedGroup;
        }

        Object userObject = node.getUserObject();
        if (userObject instanceof GroupNode groupNode) {
            return groupNode.path();
        }

        return selectedGroup;
    }

    private void moveGroupBranch(String sourceGroupPath, String targetParentGroupPath) {
        String normalizedSource = normalizeGroupPath(sourceGroupPath);
        String normalizedTargetParent = normalizeGroupPath(targetParentGroupPath);

        if (normalizedSource.isBlank()
                || normalizedSource.equalsIgnoreCase(VaultService.DEFAULT_GROUP)
                || normalizedSource.equalsIgnoreCase(normalizedTargetParent)
                || normalizedTargetParent.toLowerCase().startsWith(normalizedSource.toLowerCase() + "/")) {
            return;
        }

        if (!session.moveGroup(normalizedSource, normalizedTargetParent)) {
            JOptionPane.showMessageDialog(this, "Skupinu se nepodařilo přesunout.", "Chyba", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String movedGroupPath = normalizedTargetParent.isBlank() || VaultService.DEFAULT_GROUP.equalsIgnoreCase(normalizedTargetParent)
                ? leafName(normalizedSource)
                : normalizedTargetParent + "/" + leafName(normalizedSource);
        refreshViews(movedGroupPath, null);
        selectGroupInTree(movedGroupPath);
        setStatus("Skupina byla přesunuta.");
    }

    private static String parentPath(String groupName) {
        String normalized = normalizeGroupPath(groupName);
        int separatorIndex = normalized.lastIndexOf('/');
        return separatorIndex >= 0 ? normalized.substring(0, separatorIndex) : "";
    }

    private static String leafName(String groupName) {
        String normalized = normalizeGroupPath(groupName);
        int separatorIndex = normalized.lastIndexOf('/');
        return separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
    }

    private static String normalizeGroupPath(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return VaultService.DEFAULT_GROUP;
        }

        String[] segments = groupName.trim().split("/");
        List<String> normalizedSegments = new ArrayList<>();
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                normalizedSegments.add(trimmed);
            }
        }

        if (normalizedSegments.isEmpty()) {
            return VaultService.DEFAULT_GROUP;
        }

        return String.join("/", normalizedSegments);
    }

    private String readPasswordField() {
        char[] passwordChars = passwordField.getPassword();
        try {
            return new String(passwordChars);
        } finally {
            CryptoUtils.wipe(passwordChars);
        }
    }

    private static void copyToClipboard(String value) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private enum ValueKind {
        SERVER_NAME,
        USERNAME,
        PASSWORD
    }

    private final class GroupTreeTransferHandler extends TransferHandler {
        @Override
        protected Transferable createTransferable(JComponent component) {
            String sourceGroupPath = getSelectedTreeGroupPath();
            if (sourceGroupPath == null || sourceGroupPath.isBlank() || VaultService.DEFAULT_GROUP.equalsIgnoreCase(sourceGroupPath)) {
                return null;
            }
            return new StringSelection(sourceGroupPath);
        }

        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDrop() && support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                String sourceGroupPath = (String) support.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
                TreePath dropPath = ((javax.swing.JTree.DropLocation) support.getDropLocation()).getPath();
                String targetParentGroupPath = dropPath == null ? "" : treePathToGroupPath(dropPath);

                if (sourceGroupPath == null || sourceGroupPath.isBlank()) {
                    return false;
                }

                if (targetParentGroupPath.equalsIgnoreCase(sourceGroupPath)
                        || targetParentGroupPath.toLowerCase().startsWith(sourceGroupPath.toLowerCase() + "/")) {
                    return false;
                }

                moveGroupBranch(sourceGroupPath, targetParentGroupPath);
                return true;
            } catch (IOException | java.awt.datatransfer.UnsupportedFlavorException ex) {
                JOptionPane.showMessageDialog(PasswordVaultFrame.this, "Přesun se nepodařilo dokončit.", "Chyba", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
    }

    private String treePathToGroupPath(TreePath treePath) {
        Object lastComponent = treePath.getLastPathComponent();
        if (lastComponent instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof GroupNode groupNode) {
            return groupNode.path();
        }
        return "";
    }

    private record GroupNode(String label, String path) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final class CredentialTableModel extends AbstractTableModel {
        private final List<VaultService.DecryptedCredential> entries = new ArrayList<>();
        private final String[] columnNames = {"Server name", "username", "password"};

        void setEntries(List<VaultService.DecryptedCredential> newEntries) {
            entries.clear();
            entries.addAll(newEntries);
            fireTableDataChanged();
        }

        VaultService.DecryptedCredential getEntryAt(int rowIndex) {
            return entries.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            VaultService.DecryptedCredential credential = entries.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> credential.serverName();
                case 1 -> credential.username();
                case 2 -> maskPassword(credential.password());
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        private static String maskPassword(String password) {
            if (password == null || password.isEmpty()) {
                return "";
            }
            return "*".repeat(password.length());
        }
    }
}
