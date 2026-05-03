package org.easyrpg.player.player;

import java.util.ArrayList;

public class DirectoryTree {
    public final String[] names;
    public final boolean[] types;

    public DirectoryTree(ArrayList<String> files, ArrayList<Boolean> isDirectory) {
        if (files.size() != isDirectory.size()) {
            throw new IllegalArgumentException("Directory entry size mismatch");
        }

        names = files.toArray(new String[0]);
        types = new boolean[isDirectory.size()];
        for (int i = 0; i < isDirectory.size(); i++) {
            types[i] = isDirectory.get(i);
        }
    }
}
