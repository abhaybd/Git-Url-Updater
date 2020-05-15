package com.coolioasjulio.urlupdater;

import com.alee.laf.WebLookAndFeel;
import com.alee.skin.dark.WebDarkSkin;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        WebLookAndFeel.install(WebDarkSkin.class);
        Main main = new Main();
        SwingUtilities.invokeLater(main::start);
    }

    private GUI gui;
    private File rootDir;
    private volatile boolean allowFind = true; // flag to disable find operation without disabling button
    private volatile File[] repos;

    public Main() {
        rootDir = new File(System.getProperty("user.home"));
        try {
            SwingUtilities.invokeAndWait(this::initUI);
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        gui = new GUI();

        gui.rootDirLabel.setText(rootDir.getAbsolutePath());

        gui.chooseRootDirButton.addActionListener(this::onChooseRootDir);
        gui.updateRepositoryUrlsButton.addActionListener(this::onUpdateRepositoryUrls);
        gui.findRepositoriesButton.addActionListener(this::onFindRepos);

        gui.setNumFoundRepos(-1);

        Dimension d = gui.root.getPreferredSize();
        gui.root.setPreferredSize(new Dimension(d.width * 2, d.height));

        ToolTipManager.sharedInstance().setInitialDelay(1500);
    }

    private void onFindRepos(ActionEvent e) {
        if (rootDir == null || !rootDir.isDirectory() || rootDir.listFiles() == null) {
            showError("Invalid root directory!");
        } else if (allowFind) {
            allowFind = false;
            gui.updateRepositoryUrlsButton.setEnabled(false);
            gui.chooseRootDirButton.setEnabled(false);
            gui.foundReposArea.setText("Loading...");
            CompletableFuture<File[]> future = RepoUtils.findRepositoriesAsync(rootDir);
            future.thenAccept(f -> {
                allowFind = true;
                repos = f;
                SwingUtilities.invokeLater(() -> gui.chooseRootDirButton.setEnabled(true));
                gui.displayFoundRepos(Arrays.stream(f).map(File::getAbsolutePath).toArray(String[]::new));
                gui.setUpdateButtonEnabled(true);
            });
        }
    }

    private void onChooseRootDir(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int ret = chooser.showOpenDialog(gui.root);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.isDirectory() && file.listFiles() != null) {
                rootDir = file;
                gui.setRootDir(file.getAbsolutePath());
                gui.setNumFoundRepos(-1);
                gui.foundReposArea.setText("");
            } else {
                showError("Invalid root directory!");
            }
        }
    }

    private void onUpdateRepositoryUrls(ActionEvent e) {
        String prevUsername = gui.prevUsernameField.getText().trim();
        String newUsername = gui.newUsernameField.getText().trim();
        File[] repos = this.repos;
        if (prevUsername.length() == 0) {
            showError("Invalid previous username! Please enter your previous GitHub username!");
        } else if (newUsername.length() == 0) {
            showError("Invalid new username! Please enter your new GitHub username!");
        } else if (repos == null || repos.length == 0) {
            showError("Please find some repositories before updating urls!");
        } else {
            gui.setUpdateButtonEnabled(false);
            CompletableFuture<Integer> future =
                    RepoUtils.updateRepositoryUrlsAsync(repos, prevUsername, newUsername);
            future.thenAccept(i -> {
                        gui.setUpdateButtonEnabled(true);
                        JOptionPane.showMessageDialog(gui.root,
                                String.format("Successfully updated the urls of %d repositories.", i),
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    });
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(gui.root, message, "Error!", JOptionPane.ERROR_MESSAGE);
    }

    public void start() {
        JFrame frame = new JFrame("Git Repo URL Updater");
        frame.add(gui.root);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
