package com.ppolivka.gitlabprojects.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.ValidationInfo;
import com.ppolivka.gitlabprojects.api.dto.ServerDto;
import com.ppolivka.gitlabprojects.common.ReadOnlyTableModel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Dialog for GitLab setting configuration
 *
 * @author ppolivka
 * @since 27.10.2015
 */
public class SettingsView implements SearchableConfigurable {

    public static final String DIALOG_TITLE = "GitLab Settings";
    SettingsState settingsState = SettingsState.getInstance();
    ExecutorService executor = Executors.newSingleThreadExecutor();

    private JPanel mainPanel;
    private JTextField textHost;
    private JTextField textAPI;
    private JButton apiHelpButton;
    private JCheckBox defaultRemoveBranch;
    private JTable serverTable;
    private JButton addNewOneButton;
    private JButton editButton;
    private JButton deleteButton;

    public void setup() {
        onServerChange();
        textHost.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onServerChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onServerChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onServerChange();
            }
        });
        apiHelpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openWebPage(generateHelpUrl());
            }
        });
        addNewOneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ServerDto serverDto = new ServerDto();
                serverDto.setHost(textHost.getText());
                serverDto.setToken(textAPI.getText());
                serverDto.setDefaultRemoveBranch(defaultRemoveBranch.isSelected());
                settingsState.addServer(serverDto);
                reset();
            }
        });
        deleteButton.addActionListener(e -> {
            ServerDto serverDto = getSelectedServer();
            if(serverDto != null) {
                settingsState.deleteServer(serverDto);
                reset();
            }
        });
        editButton.addActionListener(e -> {
            ServerDto serverDto = getSelectedServer();
            if(serverDto != null) {
                textHost.setText(serverDto.getHost());
                textAPI.setText(serverDto.getToken());
                defaultRemoveBranch.setSelected(serverDto.isDefaultRemoveBranch());
            }
        });
    }

    private ServerDto getSelectedServer() {
        if(serverTable.getSelectedRow() >= 0) {
            String host = (String) serverTable.getValueAt(serverTable.getSelectedRow(), 0);
            String token = (String) serverTable.getValueAt(serverTable.getSelectedRow(), 1);
            boolean mergeDefault = (Boolean) serverTable.getValueAt(serverTable.getSelectedRow(), 2);
            ServerDto serverDto = new ServerDto();
            serverDto.setHost(host);
            serverDto.setToken(token);
            serverDto.setDefaultRemoveBranch(mergeDefault);
            return serverDto;
        }
        return null;
    }

    private TableModel serverModel(Collection<ServerDto> servers) {
        Object[] columnNames = {"Server", "Token", "Default merged"};
        Object[][] data = new Object[servers.size()][columnNames.length];
        int i = 0;
        for (ServerDto server : servers) {
            Object[] row = new Object[columnNames.length];
            row[0] = server.getHost();
            row[1] = server.getToken();
            row[2] = server.isDefaultRemoveBranch();
            data[i] = row;
            i++;
        }
        return new ReadOnlyTableModel(data, columnNames);
    }

    @Nullable
    public ValidationInfo doValidate(long timeout) {
        final String hostText = textHost.getText();
        final String apiText = textAPI.getText();
        if(StringUtils.isBlank(hostText) && StringUtils.isBlank(apiText) && settingsState.getServers().size() > 0) {
            return null;
        }
        try {
            if (isModified() && isNotBlank(hostText) && isNotBlank(apiText)) {
                if (!isValidUrl(hostText)) {
                    return new ValidationInfo(SettingError.NOT_A_URL.message(), textHost);
                } else {

                    Future<ValidationInfo> infoFuture = executor.submit(() -> {
                        try {
                            settingsState.isApiValid(hostText, apiText);
                            return null;
                        } catch (UnknownHostException e) {
                            return new ValidationInfo(SettingError.SERVER_CANNOT_BE_REACHED.message(), textHost);
                        } catch (IOException e) {
                            return new ValidationInfo(SettingError.INVALID_API_TOKEN.message(), textAPI);
                        }
                    });
                    try {
                        ValidationInfo info = infoFuture.get(timeout, TimeUnit.MILLISECONDS);
                        return info;
                    } catch (Exception e) {
                        return new ValidationInfo(SettingError.GENERAL_ERROR.message());
                    }
                }
            }
        } catch (Exception e) {
            return new ValidationInfo(SettingError.GENERAL_ERROR.message());
        }
        return null;
    }

    //region Searchable Configurable interface methods
    @NotNull
    @Override
    public String getId() {
        return DIALOG_TITLE;
    }

    @Nullable
    @Override
    public Runnable enableSearch(String s) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return DIALOG_TITLE;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        reset();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        String[] save = save();
        return save == null
                || !save[0].equals(settingsState.getHost())
                || !save[1].equals(settingsState.getToken())
                || defaultRemoveBranch.isSelected() != settingsState.isDefaultRemoveBranch();
    }

    @Override
    public void apply() throws ConfigurationException {
        String[] save = save();
        settingsState.setHost(save[0]);
        settingsState.setToken(save[1]);
        settingsState.setDefaultRemoveBranch(defaultRemoveBranch.isSelected());
    }

    @Override
    public void reset() {
        fill(settingsState);
    }

    @Override
    public void disposeUIResources() {

    }
    //endregion

    //region Editable View interface methods
    public void fill(SettingsState settingsState) {
        textHost.setText(settingsState == null ? "" : settingsState.getHost());
        textAPI.setText(settingsState == null ? "" : settingsState.getToken());
        defaultRemoveBranch.setSelected(settingsState == null ? true : settingsState.isDefaultRemoveBranch());
        serverTable.setModel(serverModel(settingsState.getServers()));
        serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverTable.getSelectionModel().addListSelectionListener(event -> {
            editButton.setEnabled(true);
            deleteButton.setEnabled(true);
        });

    }

    public String[] save() {
        return new String[]{textHost.getText(), textAPI.getText()};
    }
    //endregion

    //region Private methods
    private String generateHelpUrl() {
        final String hostText = textHost.getText();
        StringBuilder helpUrl = new StringBuilder();
        helpUrl.append(hostText);
        if (!hostText.endsWith("/")) {
            helpUrl.append("/");
        }
        helpUrl.append("profile/personal_access_tokens");
        return helpUrl.toString();
    }

    private void onServerChange() {
        ValidationInfo validationInfo = doValidate(500);
        if (validationInfo == null || (validationInfo != null && !validationInfo.message.equals(SettingError.NOT_A_URL.message))) {
            apiHelpButton.setEnabled(true);
            apiHelpButton.setToolTipText("API Key can be find in your profile setting inside GitLab Server: \n" + generateHelpUrl());
            addNewOneButton.setEnabled(true);
        } else {
            apiHelpButton.setEnabled(false);
            addNewOneButton.setEnabled(false);
        }
    }

    private static boolean isValidUrl(String s) {
        Pattern urlPattern = Pattern.compile("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher matcher = urlPattern.matcher(s);
        return matcher.matches();
    }

    private static void openWebPage(String uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(uri));
            } catch (Exception ignored) {
            }
        }
    }
    //endregion
}