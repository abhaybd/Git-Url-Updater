package com.coolioasjulio.urlupdater;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }

    private GUI gui;
    private File rootDir;
    private volatile boolean allowFind = true; // flag to disable find operation without disabling button
    private volatile File[] repos;
    public Main() {
        gui = new GUI();

        rootDir = new File(System.getProperty("user.home"));
        SwingUtilities.invokeLater(this::initUI);
    }

    private void initUI() {
        gui.rootDirLabel.setText(rootDir.getAbsolutePath());
        gui.foundReposArea.setEditable(false);

        gui.chooseRootDirButton.addActionListener(this::onChooseRootDir);
        gui.updateRepositoryUrlsButton.addActionListener(this::onUpdateRepositoryUrls);
        gui.findRepositoriesButton.addActionListener(this::onFindRepos);

        gui.setNumFoundRepos(-1);

        Dimension d = gui.root.getPreferredSize();
        gui.root.setPreferredSize(new Dimension(d.width * 2, d.height));
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

    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(gui.root, "Error!", message, JOptionPane.ERROR_MESSAGE);
    }

    public void start() {
        JFrame frame = new JFrame("Git Repo URL Updater");
        frame.add(gui.root);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
