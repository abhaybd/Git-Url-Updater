package com.coolioasjulio.urlupdater;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class RepoUtils {
    private static final String GIT_NAME = ".git";

    public static void updateRepositoryUrls(File[] repoRoots, String prevUsername, String newUsername) {
        CountDownLatch latch = new CountDownLatch(repoRoots.length);
        ExecutorService service = Executors.newCachedThreadPool();
        for (File root : repoRoots) {
            //TODO: use JGit to update urls
        }
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
