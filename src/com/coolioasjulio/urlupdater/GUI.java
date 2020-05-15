package com.coolioasjulio.urlupdater;

import javax.swing.*;

public class GUI {
    public JTextField prevUsernameField;
    public JTextField newUsernameField;
    public JButton chooseRootDirButton;
    public JTextArea foundReposArea;
    public JButton updateRepositoryUrlsButton;
    public JLabel rootDirLabel;
    public JLabel numReposLabel;
    public JPanel root;
    public JButton findRepositoriesButton;

    public void setRootDir(String path) {
        SwingUtilities.invokeLater(() ->  rootDirLabel.setText(String.format("Currently selected root: %s", path)));

    }

    public void setNumFoundRepos(int num) {
        SwingUtilities.invokeLater(() -> {
            if (num != -1) {
                numReposLabel.setText(String.format("Found %d repos", num));
            } else {
                numReposLabel.setText(String.format("Click \"%s\" to detect repositories!", findRepositoriesButton.getText()));
            }
        });
    }

    public void displayFoundRepos(String[] paths) {
        setNumFoundRepos(paths.length);
        SwingUtilities.invokeLater(() -> foundReposArea.setText(String.join("\n", paths)));
    }

    public void setUpdateButtonEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> updateRepositoryUrlsButton.setEnabled(enabled));
    }
}
