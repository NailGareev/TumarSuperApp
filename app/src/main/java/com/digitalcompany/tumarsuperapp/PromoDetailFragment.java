package com.digitalcompany.tumarsuperapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PromoDetailFragment extends Fragment {

    private static final String ARG_TAG   = "promo_tag";
    private static final String ARG_TITLE = "promo_title";
    private static final String ARG_SUB   = "promo_subtitle";
    private static final String ARG_BADGE = "promo_badge";
    private static final String ARG_S1V   = "stat1_value";
    private static final String ARG_S1L   = "stat1_label";
    private static final String ARG_S2V   = "stat2_value";
    private static final String ARG_S2L   = "stat2_label";
    private static final String ARG_S3V   = "stat3_value";
    private static final String ARG_S3L   = "stat3_label";
    private static final String ARG_DESC  = "description";
    private static final String ARG_TERMS = "terms";

    public static PromoDetailFragment newInstance(
            String tag, String title, String subtitle, String badge,
            String stat1V, String stat1L, String stat2V, String stat2L,
            String stat3V, String stat3L, String description, String terms) {
        PromoDetailFragment f = new PromoDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAG, tag);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUB, subtitle);
        args.putString(ARG_BADGE, badge);
        args.putString(ARG_S1V, stat1V);
        args.putString(ARG_S1L, stat1L);
        args.putString(ARG_S2V, stat2V);
        args.putString(ARG_S2L, stat2L);
        args.putString(ARG_S3V, stat3V);
        args.putString(ARG_S3L, stat3L);
        args.putString(ARG_DESC, description);
        args.putString(ARG_TERMS, terms);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_promo_detail, container, false);

        Bundle args = getArguments();
        if (args == null) return view;

        ((TextView) view.findViewById(R.id.text_detail_badge)).setText(args.getString(ARG_BADGE, ""));
        ((TextView) view.findViewById(R.id.text_detail_title)).setText(args.getString(ARG_TITLE, ""));
        ((TextView) view.findViewById(R.id.text_detail_subtitle)).setText(args.getString(ARG_SUB, ""));
        ((TextView) view.findViewById(R.id.text_stat1_value)).setText(args.getString(ARG_S1V, ""));
        ((TextView) view.findViewById(R.id.text_stat1_label)).setText(args.getString(ARG_S1L, ""));
        ((TextView) view.findViewById(R.id.text_stat2_value)).setText(args.getString(ARG_S2V, ""));
        ((TextView) view.findViewById(R.id.text_stat2_label)).setText(args.getString(ARG_S2L, ""));
        ((TextView) view.findViewById(R.id.text_stat3_value)).setText(args.getString(ARG_S3V, ""));
        ((TextView) view.findViewById(R.id.text_stat3_label)).setText(args.getString(ARG_S3L, ""));
        ((TextView) view.findViewById(R.id.text_detail_description)).setText(args.getString(ARG_DESC, ""));
        ((TextView) view.findViewById(R.id.text_detail_terms)).setText(args.getString(ARG_TERMS, ""));

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        view.findViewById(R.id.btn_participate).setOnClickListener(v ->
                Toast.makeText(getContext(), "Вы успешно подали заявку на участие!", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
