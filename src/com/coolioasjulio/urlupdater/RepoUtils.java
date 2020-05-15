package com.coolioasjulio.urlupdater;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

public class RepoUtils {
    private static final String GIT_NAME = ".git";

    public static CompletableFuture<Integer> updateRepositoryUrlsAsync(File[] repoRoots, String prevUsername, String newUsername) {
        return null; // TODO: implement
    }

    public static int updateRepositoryUrls(File[] repoRoots, String prevUsername, String newUsername) {
        int numUpdated = 0;
        // Iterate through each repository
        for (File root : repoRoots) {
            try {
                // Create the JGit objects to manipulate the repository
                File gitDir = new File(root, GIT_NAME);
                Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
                Git git = new Git(repo);
                List<RemoteConfig> remotes = git.remoteList().call();
                boolean updated = false;
                // For each remote in this repo, update it with the new username if necessary
                for (RemoteConfig remote : remotes) {
                    updated |= updateRemote(remote, repo.getConfig(), prevUsername, newUsername);
                }
                // Save the repo config to storage
                if (updated) {
                    numUpdated++;
                    repo.getConfig().save();
                }
            } catch (IOException | GitAPIException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return numUpdated;
    }

    private static boolean updateRemote(RemoteConfig remote, Config config, String prevUsername, String newUsername) throws URISyntaxException {
        boolean updatedRemote = false;
        List<URIish> uris = new ArrayList<>(remote.getURIs());
        // Iterate through each uri in this repo
        for (URIish uri : uris) {
            // Get the username and repo name from the uri
            String[] parts = explodePath(uri); // [username, repo name]
            // If this uri points to the old username, update the path to the new username
            if (parts[0].equals(prevUsername)) {
                URIish replacement = uri.setRawPath(createPath(newUsername, parts[1]));
                remote.removeURI(uri);
                remote.addURI(replacement);
                updatedRemote = true;
            }
        }
        // If we've modified this remote, update the config
        if (updatedRemote) {
            remote.update(config);
        }
        return updatedRemote;
    }

    private static String[] explodePath(URIish uri) {
        String path = uri.getRawPath();
        if (path.startsWith("/")) path = path.substring(1);
        return path.split("/", 2);
    }

    private static String createPath(String username, String repository) {
        return String.format("/%s/%s", username, repository);
    }

    public static File[] findRepositories(File root) {
        if (!root.isDirectory()) throw new IllegalArgumentException("Invalid root directory!");
        return findRepos(root).toArray(File[]::new);
    }

    public static CompletableFuture<File[]> findRepositoriesAsync(File root) {
        final CompletableFuture<File[]> future = new CompletableFuture<>();
        Thread t = new Thread(() -> {
            File[] files = findRepositories(root);
            future.complete(files);
        });
        t.setDaemon(true);
        t.start();
        return future;
    }

    private static Stream<File> findRepos(File dir) {
        // for each subdir find repos in that subdir
        File[] files = dir.listFiles();
        if (files == null) return Stream.empty();
        if (isRepo(dir)) return Stream.of(dir);
        return Arrays.stream(files) // get sub files + dirs
                .parallel() // make stream parallel
                .filter(File::isDirectory) // filter for only subdirs
                .flatMap(RepoUtils::findRepos); // recursively find repos in this subdir
    }

    private static boolean isRepo(File dir) {
        if (!dir.isDirectory()) throw new IllegalStateException(dir.getAbsolutePath() + " is not a directory!");
        File[] subs = Objects.requireNonNull(dir.listFiles());
        for (File f : subs) {
            if (f.isDirectory() && f.getName().equals(GIT_NAME)) return true;
        }
        return false;
    }
}
