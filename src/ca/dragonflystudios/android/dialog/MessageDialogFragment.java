package ca.dragonflystudios.android.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class MessageDialogFragment extends DialogFragment
{
    private static final String TITLE_KEY   = "title";
    private static final String MESSAGE_KEY = "message";

    public static MessageDialogFragment newInstance(int titleId, int messageId) {
        MessageDialogFragment mdf = new MessageDialogFragment();
        Bundle args = new Bundle();
        args.putInt(TITLE_KEY, titleId);
        args.putInt(MESSAGE_KEY, messageId);
        mdf.setArguments(args);

        return mdf;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        int titleId = args.getInt(TITLE_KEY);
        int messageId = args.getInt(MESSAGE_KEY);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleId).setMessage(messageId)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }
}
