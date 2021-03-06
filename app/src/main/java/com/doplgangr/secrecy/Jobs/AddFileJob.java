package com.doplgangr.secrecy.Jobs;

import android.content.Context;
import android.net.Uri;

import com.doplgangr.secrecy.FileSystem.File;
import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.Util;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import de.greenrobot.event.EventBus;

import static com.ipaulpro.afilechooser.utils.FileUtils.getPath;

public class AddFileJob extends Job {
    public static final int PRIORITY = 9;   //High. Lower than UI jobs
    private Vault vault;
    private Uri data;
    private Context context;
    private Boolean alreadyDeleting = false;

    public AddFileJob(Context context, Vault vault, Uri uri) {
        super(new Params(PRIORITY)
                .groupBy(uri.toString()));
        this.vault = vault;
        this.data = uri;
        this.context = context;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Util.log("Adding file", data);
        if (!alreadyDeleting) {
            String filename = vault.addFile(context, data);
            Uri thumbnail = storage.saveThumbnail(context, data, filename);
            if (thumbnail != null) {
                vault.addFile(context, thumbnail);
                storage.purgeFile(new java.io.File(thumbnail.getPath()));
            }
            File returnedFile = vault.getFileInstance(filename);
            EventBus.getDefault().post(new NewFileEvent(returnedFile));
            alreadyDeleting = true;
        }
        java.io.File actualFile = new java.io.File(getPath(context, data));
        storage.purgeFile(actualFile, data); //Try to delete original file.
    }

    @Override
    protected void onCancel() {
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        //ignore everything and rerun
        throwable.printStackTrace();
        return true;
    }

    @Override
    protected int getRetryLimit() {
        return 5;  //Shouldn't try too much.
    }

    public class NewFileEvent {
        public File file;

        public NewFileEvent(File file) {
            this.file = file;
        }
    }


}