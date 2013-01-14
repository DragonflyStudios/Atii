package ca.dragonflystudios.android.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class WarningDialogFragment extends DialogFragment
{
    public interface WarningDialogListener
    {
        public void onPositive(WarningDialogFragment wdf);

        public void onNegative(WarningDialogFragment wdf);
    }

    private static final String TITLE_KEY   = "title";
    private static final String MESSAGE_KEY = "message";

    public static WarningDialogFragment newInstance(int titleId, int messageId) {
        WarningDialogFragment wdf = new WarningDialogFragment();
        Bundle args = new Bundle();
        args.putInt(TITLE_KEY, titleId);
        args.putInt(MESSAGE_KEY, messageId);
        wdf.setArguments(args);

        return wdf;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (WarningDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DeleteDialogListener");
        }
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
                        mListener.onPositive(WarningDialogFragment.this);
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onNegative(WarningDialogFragment.this);
                    }
                });
        return builder.create();
    }

    private WarningDialogListener mListener;
}
