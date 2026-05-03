package org.easyrpg.player.player;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class SafFile {
    private static final String TAG = "EasyRPG-SAF";

    private final Context context;
    private final Uri uri;

    private boolean metadataPopulated;
    private boolean metaExists;
    private boolean metaIsFile;
    private long metaFileSize;

    private SafFile(Context context, Uri uri) {
        this.context = context;
        this.uri = uri;
    }

    public static SafFile fromPath(Context context, String path) {
        if (context == null || path == null || path.isEmpty()) {
            return null;
        }

        String normalized = path.startsWith("content://") ? path : "content://" + path;
        return new SafFile(context.getApplicationContext(), Uri.parse(normalized));
    }

    public boolean isFile() {
        populateMetadata();
        return metaExists && metaIsFile;
    }

    public boolean isDirectory() {
        populateMetadata();
        return metaExists && !metaIsFile;
    }

    public boolean exists() {
        populateMetadata();
        return metaExists;
    }

    public long getFilesize() {
        populateMetadata();
        return metaFileSize;
    }

    public int createInputFileDescriptor() {
        try (ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(uri, "r")) {
            return fd == null ? -1 : fd.detachFd();
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Failed to open SAF input descriptor: " + uri, e);
            return -1;
        }
    }

    public int createOutputFileDescriptor(boolean append) {
        String mode = append ? "wa" : "wt";
        try (ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(uri, mode)) {
            return fd == null ? -1 : fd.detachFd();
        } catch (IOException | RuntimeException e) {
            if (!append) {
                try (ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(uri, "w")) {
                    return fd == null ? -1 : fd.detachFd();
                } catch (IOException | RuntimeException ignored) {
                    // Fall through to the logged failure below.
                }
            }
            Log.w(TAG, "Failed to open SAF output descriptor: " + uri, e);
            return -1;
        }
    }

    DirectoryTree getDirectoryContent() {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Boolean> directories = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        try {
            String documentId = DocumentsContract.isDocumentUri(context, uri)
                ? DocumentsContract.getDocumentId(uri)
                : DocumentsContract.getTreeDocumentId(uri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId);

            try (Cursor cursor = resolver.query(
                childrenUri,
                new String[] {
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null,
                null,
                null
            )) {
                if (cursor == null) {
                    return new DirectoryTree(names, directories);
                }
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String mimeType = cursor.getString(1);
                    if (name != null) {
                        names.add(name);
                        directories.add(DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType));
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to list SAF directory: " + uri, e);
        }

        return new DirectoryTree(names, directories);
    }

    private void populateMetadata() {
        if (metadataPopulated) {
            return;
        }

        metadataPopulated = true;
        try (Cursor cursor = context.getContentResolver().query(
            uri,
            new String[] {
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            },
            null,
            null,
            null
        )) {
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            metaExists = true;
            String mimeType = cursor.getString(0);
            metaIsFile = !DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
            metaFileSize = cursor.isNull(1) ? 0L : cursor.getLong(1);
        } catch (RuntimeException e) {
            metaExists = false;
        }
    }
}
