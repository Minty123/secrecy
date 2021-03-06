/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.doplgangr.secrecy.FileSystem;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.Jobs.DeleteFileJob;
import com.doplgangr.secrecy.Jobs.ShredFileJob;
import com.doplgangr.secrecy.Util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.crypto.CipherInputStream;

import static com.ipaulpro.afilechooser.utils.FileUtils.getPath;

public class storage {

    public static void DeleteRecursive(java.io.File directory) {
        try {
            Collection files = FileUtils.listFiles(directory, null, true);
            for (Object file : files)
                purgeFile((File) file);
        } catch (Exception e) {
            Util.log(e);
        } finally {
            try {
                FileUtils.cleanDirectory(directory);
            } catch (Exception ignored) {
            }
        }
    }

    public static void purgeFile(java.io.File file) {
        purgeFile(file, null);
    }

    public static void purgeFile(java.io.File file, Uri uri) {        //Starts a job to do the real
        DeleteFileJob job = new DeleteFileJob(file);
        job.addURI(uri);
        CustomApp.jobManager.addJobInBackground(job); // deletion in background
    }

    public static void shredFile(OutputStream fileOS, long size) {
        CustomApp.jobManager.addJobInBackground(new ShredFileJob(fileOS, size));
    }

    public static java.io.File getTempFolder() {
        java.io.File tempDir = CustomApp.context.getExternalCacheDir();
        if (tempDir == null)                                                // when all else fails
            tempDir = CustomApp.context.getFilesDir();
        try {
            FileUtils.forceMkdir(tempDir);
        } catch (Exception e) {
            Util.log(e);
        }
        return tempDir;
    }

    private static String getAbsTempFolder() {
        return getTempFolder().getAbsolutePath() + "/";
    }

    private static String ROOT() {
        SharedPreferences settings = CustomApp.context.getSharedPreferences(Config.settingsStore, 0);
        return settings.getString(Config.root, Environment.getExternalStorageDirectory()
                .getAbsoluteFile()
                + "/SECRECYFILES");
    }

    public static java.io.File getRoot() {
        java.io.File tempDir = new java.io.File(ROOT());
        try {
            FileUtils.forceMkdir(tempDir);
        } catch (Exception e) {
            Util.log(e);
        }
        return tempDir;
    }

    public static ArrayList<java.io.File> getDirectories(File folder) {
        File[] subfiles = folder.listFiles();
        ArrayList<File> subfolders = new ArrayList<File>();
        for (java.io.File subfolder : subfiles)
            if (subfolder.isDirectory())
                subfolders.add(subfolder);
        return subfolders;
    }

    public static Boolean setRoot(String root) {
        SharedPreferences settings = CustomApp.context.getSharedPreferences(Config.settingsStore, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Config.root, root);
        return editor.commit();
    }

    public static void deleteTemp() {
        DeleteRecursive(getTempFolder());                           //Delete temp files generated
        DeleteRecursive(CustomApp.context.getCacheDir());           //Delete App cache
        DeleteRecursive(CustomApp.context.getExternalCacheDir());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            for (java.io.File externalCacheDir : CustomApp.context.getExternalCacheDirs())
                DeleteRecursive(externalCacheDir);                      //Just to be sure
    }

    public static Uri saveThumbnail(Context context, Uri uri, String filename) {
        InputStream stream = null;
        try {
            stream = context.getContentResolver().openInputStream(uri);
            java.io.File thumbpath = new java.io.File(getAbsTempFolder() + "/" + "_thumb" + filename);
            if (thumbpath.exists())
                storage.purgeFile(thumbpath);
            thumbpath.createNewFile();
            FileOutputStream out = new FileOutputStream(thumbpath);
            Bitmap bitmap = decodeSampledBitmapFromPath(getPath(context, uri), 150, 150);
            if (bitmap == null) {   //If photo fails, try Video
                Util.log(getPath(context, uri));
                bitmap = ThumbnailUtils.createVideoThumbnail(
                        getPath(context, uri), MediaStore.Video.Thumbnails.MICRO_KIND);
            }
            if (bitmap == null) {
                out.close();
                storage.purgeFile(thumbpath);
                return null;
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            return Uri.fromFile(thumbpath);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        return null;
    }

    public static Bitmap getThumbnailfromStream(CipherInputStream streamThumb) {
        if (streamThumb != null)
            return BitmapFactory.decodeStream(streamThumb);
        return null;
    }

    public static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth,
                                                     int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bmp = BitmapFactory.decodeFile(path, options);
        return bmp;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }
}
