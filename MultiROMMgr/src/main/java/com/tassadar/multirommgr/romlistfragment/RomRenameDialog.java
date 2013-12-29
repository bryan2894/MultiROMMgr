package com.tassadar.multirommgr.romlistfragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tassadar.multirommgr.MainActivity;
import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.StatusAsyncTask;

public class RomRenameDialog extends DialogFragment implements View.OnClickListener {

    private static class RomNameFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            boolean keepOriginal = true;
            StringBuilder sb = new StringBuilder(end - start);
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (isAllowed(c, dstart + i))
                    sb.append(c);
                else
                    keepOriginal = false;
            }
            if (keepOriginal)
                return null;
            else {
                if (source instanceof Spanned) {
                    SpannableString sp = new SpannableString(sb);
                    TextUtils.copySpansFrom((Spanned) source, start, end, null, sp, 0);
                    return sp;
                } else {
                    return sb;
                }
            }
        }

        private static boolean isAllowed(char c, int pos) {
            return c != '/' && c != '\\' && (pos != 0 || c != '.');
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity a = getActivity();
        Bundle args = getArguments();

        LinearLayout view = (LinearLayout)a.getLayoutInflater()
                .inflate(R.layout.dialog_rename_rom, null, false);

        TextView t = (TextView) view.findViewById(R.id.rom_name);
        t.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(MultiROM.MAX_ROM_NAME),
                new RomNameFilter()
        });
        t.setText(args.getString("rom_name"));

        AlertDialog.Builder b = new AlertDialog.Builder(a);

        return b.setView(view)
                .setTitle(R.string.rename_rom)
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .setPositiveButton(R.string.rename, null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog d = (AlertDialog)getDialog();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        MultiROM m = StatusAsyncTask.instance().getMultiROM();
        Bundle args = getArguments();
        AlertDialog d = (AlertDialog)getDialog();

        EditText t = (EditText)d.findViewById(R.id.rom_name);
        TextView err_text = (TextView)d.findViewById(R.id.error_text);

        String new_name = t.getText().toString();
        String old_name = args.getString("rom_name");

        if(new_name.length() == 0) {
            err_text.setVisibility(View.VISIBLE);
            err_text.setText(R.string.rom_name_empty);
            return;
        }

        if(new_name.equals(old_name)) {
            dismiss();
            return;
        }

        if(m.getRoms().contains(new_name)) {
            err_text.setVisibility(View.VISIBLE);
            err_text.setText(R.string.rom_name_taken);
            return;
        }

        setCancelable(false);
        d.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        d.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
        d.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);

        t.setEnabled(false);
        err_text.setVisibility(View.GONE);

        new Thread(new RomRenameRunnable(old_name, new_name)).start();
    }

    private class RomRenameRunnable implements Runnable {
        private String m_old_name, m_new_name;
        public RomRenameRunnable(String old_name, String new_name) {
            m_old_name = old_name;
            m_new_name = new_name;
        }

        @Override
        public void run() {
            MultiROM m = StatusAsyncTask.instance().getMultiROM();
            m.renameRom(m_old_name, m_new_name);

            Activity a = getActivity();
            if(a == null)
                return;

            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismiss();

                    MainActivity a = (MainActivity)getActivity();
                    if(a != null)
                        a.refresh();
                }
            });
        }
    }
}
