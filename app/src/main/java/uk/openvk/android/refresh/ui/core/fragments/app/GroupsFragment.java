package uk.openvk.android.refresh.ui.core.fragments.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kieronquinn.monetcompat.core.MonetCompat;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import uk.openvk.android.refresh.Global;
import uk.openvk.android.refresh.OvkApplication;
import uk.openvk.android.refresh.R;
import uk.openvk.android.refresh.api.entities.Conversation;
import uk.openvk.android.refresh.api.entities.Group;
import uk.openvk.android.refresh.ui.core.activities.AppActivity;
import uk.openvk.android.refresh.ui.list.adapters.GroupsAdapter;
import uk.openvk.android.refresh.ui.view.layouts.ErrorLayout;
import uk.openvk.android.refresh.ui.view.layouts.ProgressLayout;

public class GroupsFragment extends Fragment {
    private View view;
    private SharedPreferences global_prefs;
    private ArrayList<Group> groups;
    private RecyclerView groupsView;
    private GroupsAdapter groupsAdapter;
    private LinearLayoutManager llm;
    private GridLayoutManager glm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Global.setInterfaceFont((AppCompatActivity) requireActivity());
        view = inflater.inflate(R.layout.fragment_groups, container, false);
        global_prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout))
                .setProgressBackgroundColorSchemeResource(R.color.navbarColor);
        ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout)).setVisibility(View.GONE);
        setTheme();
        ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout)).setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(requireActivity().getClass().getSimpleName().equals("AppActivity")) {
                    ((AppActivity) requireActivity()).refreshGroupsList(false);
                }
            }
        });
        ((ProgressLayout) view.findViewById(R.id.progress_layout)).setVisibility(View.VISIBLE);
        return view;
    }

    private void setTheme() {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorAccent,
                typedValue, true);
        if(Global.checkMonet(requireContext())) {
            MonetCompat monet = MonetCompat.getInstance();
            boolean isDarkTheme = global_prefs.getBoolean("dark_theme", false);
            if(isDarkTheme) {
                ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout)).setColorSchemeColors(
                        Global.getMonetIntColor(monet, "accent", 200));
            } else {
                ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout)).setColorSchemeColors(
                        Global.getMonetIntColor(monet, "accent", 500));
            }
        } else {
            ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout))
                    .setColorSchemeColors(typedValue.data);
        }
        if(PreferenceManager.getDefaultSharedPreferences(
                requireContext()).getBoolean("dark_theme", false)) {
            ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout))
                    .setProgressBackgroundColorSchemeColor(getResources().getColor(
                            com.google.android.material.R.color.background_material_dark));
        } else {
            ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout))
                    .setProgressBackgroundColorSchemeColor(getResources().getColor(android.R.color.white));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void createAdapter(Context ctx, ArrayList<Group> groups, String where) {
        this.groups = groups;
        groupsView = (RecyclerView) view.findViewById(R.id.groups_rv);
        if(groupsAdapter == null) {
            groupsAdapter = new GroupsAdapter(getContext(), this.groups);
            llm = new LinearLayoutManager(ctx);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            if(OvkApplication.isTablet && getResources().getConfiguration().smallestScreenWidthDp >= 760) {
                glm = new GridLayoutManager(getContext(), 3);
                groupsView.setLayoutManager(glm);
            } else if(OvkApplication.isTablet) {
                glm = new GridLayoutManager(getContext(), 2);
                groupsView.setLayoutManager(glm);
            } else {
                groupsView.setLayoutManager(llm);
            }
            groupsView.setAdapter(groupsAdapter);
        } else {
            groupsAdapter.notifyDataSetChanged();
        }
        ((ProgressLayout) view.findViewById(R.id.progress_layout)).setVisibility(View.GONE);
        ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout)).setVisibility(View.VISIBLE);
    }

    public void disableUpdateState() {
        ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout)).setRefreshing(false);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadAvatars(ArrayList<Conversation> conversations) {
        Context ctx = requireContext();
        groupsAdapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refreshAdapter() {
        if(groupsAdapter != null) {
            groupsAdapter.notifyDataSetChanged();
        }
    }

    public void showProgress() {
        ((ErrorLayout) view.findViewById(R.id.error_layout)).setVisibility(View.GONE);
        ((SwipeRefreshLayout) view.findViewById(R.id.groups_swipe_layout)).setVisibility(View.GONE);
        ((ProgressLayout) view.findViewById(R.id.progress_layout)).setVisibility(View.VISIBLE);
    }
}
