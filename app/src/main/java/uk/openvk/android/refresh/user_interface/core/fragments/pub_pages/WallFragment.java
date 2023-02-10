package uk.openvk.android.refresh.user_interface.core.fragments.pub_pages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Objects;

import uk.openvk.android.refresh.R;
import uk.openvk.android.refresh.api.models.WallPost;
import uk.openvk.android.refresh.user_interface.list.adapters.NewsfeedAdapter;

public class WallFragment extends Fragment {
    public View view;
    private RecyclerView wallView;
    public NewsfeedAdapter wallAdapter;
    private LinearLayoutManager llm;
    private ArrayList<WallPost> wallPosts;
    private RecyclerView wall_rv;
    private Context ctx;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = new Bundle();
        data.putString("createState", "ok");
        setArguments(data);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.wall_tab, container, false);
        LinearLayout loading_layout = view.findViewById(R.id.loading_layout);
        wall_rv = view.findViewById(R.id.wall_rv);
        loading_layout.setVisibility(View.VISIBLE);
        wall_rv.setVisibility(View.GONE);
        if(wallPosts != null) {
            Log.d("OpenVK", "WallPosts exist");
        } else {
            Log.d("OpenVK", "WallPosts does not exist");
        }
        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void createWallAdapter(Context ctx, ArrayList<WallPost> posts) {
        this.ctx = ctx;
        this.wallPosts = posts;
        if(wallAdapter == null) {
            wallView = wall_rv;
            llm = new LinearLayoutManager(getContext());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            wallView.setLayoutManager(llm);
            wallAdapter = new NewsfeedAdapter(ctx, this.wallPosts);
            wallView.setAdapter(wallAdapter);
        } else {
            //newsfeedAdapter.setArray(wallPosts);
            wallAdapter.notifyDataSetChanged();
        }
        LinearLayout loading_layout = view.findViewById(R.id.loading_layout);
        @SuppressLint("CutPasteId") RecyclerView wall_rv = view.findViewById(R.id.wall_rv);
        loading_layout.setVisibility(View.GONE);
        wall_rv.setVisibility(View.VISIBLE);
    }
    public NewsfeedAdapter getWallAdapter() {
        return wallAdapter;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(view != null && llm != null && wallAdapter != null) {
            LinearLayout loading_layout = view.findViewById(R.id.loading_layout);
            loading_layout.setVisibility(View.GONE);
            wall_rv = view.findViewById(R.id.wall_rv);
            wall_rv.setVisibility(View.VISIBLE);
            wall_rv.setLayoutManager(llm);
            wall_rv.setAdapter(wallAdapter);
        }
    }
}
