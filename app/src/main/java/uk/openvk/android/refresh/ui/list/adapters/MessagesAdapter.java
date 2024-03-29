package uk.openvk.android.refresh.ui.list.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.color.MaterialColors;
import com.kieronquinn.monetcompat.core.MonetCompat;

import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import uk.openvk.android.refresh.Global;
import uk.openvk.android.refresh.R;
import uk.openvk.android.refresh.api.entities.Message;
import uk.openvk.android.refresh.ui.util.glide.GlideApp;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.Holder>  {
    private final Context ctx;
    private final ArrayList<Message> items;

    public MessagesAdapter(Context context, ArrayList<Message> items, long peer_id) {
        this.ctx = context;
        this.items = items;
    }

    @NonNull
    @Override
    public MessagesAdapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == 0) {
            return new MessagesAdapter.Holder(LayoutInflater.from(ctx).inflate(
                    R.layout.msg_incoming, parent, false));
        } else if(viewType == 1) {
            return new MessagesAdapter.Holder(LayoutInflater.from(ctx).inflate(
                    R.layout.msg_outcoming, parent, false));
        } else {
            return new MessagesAdapter.Holder(LayoutInflater.from(ctx).inflate(
                    R.layout.messages_history_datestamp, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesAdapter.Holder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class Holder extends RecyclerView.ViewHolder {
        private final View convertView;
        private final TextView msg_text;
        private final TextView msg_text_2;
        private final TextView msg_timestamp;
        private final LinearLayout horizontal_layout;
        private final LinearLayout vertical_layout;

        public Holder(View view) {
            super(view);
            this.convertView = view;
            this.horizontal_layout = (LinearLayout) view.findViewById(R.id.text_layout_horizontal);
            this.vertical_layout = (LinearLayout) view.findViewById(R.id.text_layout_vertical);
            this.msg_text = (TextView) view.findViewById(R.id.msg_text);
            this.msg_timestamp = (TextView) view.findViewById(R.id.timestamp);
            this.msg_text_2 = (TextView) view.findViewById(R.id.msg_text_2);
        }

        @SuppressLint({"SimpleDateFormat", "UseCompatLoadingForDrawables"})
        void bind(final int position) {
            try {
                final Message item = getItem(position);
                assert item != null;
                if (item.type < 2) {
                    if (item.text.length() < 20) {
                        vertical_layout.setVisibility(View.GONE);
                        horizontal_layout.setVisibility(View.VISIBLE);
                        msg_text.setText(Global.formatLinksAsHtml(item.text));
                        msg_text.setMovementMethod(LinkMovementMethod.getInstance());
                    } else {
                        vertical_layout.setVisibility(View.VISIBLE);
                        horizontal_layout.setVisibility(View.GONE);
                        msg_text_2.setText(Global.formatLinksAsHtml(item.text));
                        msg_text_2.setMovementMethod(LinkMovementMethod.getInstance());
                    }
                    msg_timestamp.setText(item.timestamp);
                    if (item.type == 1) {
                        if (item.isError) {
                            ((ImageView) convertView.findViewById(R.id.error_image))
                                    .setVisibility(View.VISIBLE);
                        } else {
                            ((ImageView) convertView.findViewById(R.id.error_image))
                                    .setVisibility(View.GONE);
                        }
                        if (item.sending) {
                            ((ProgressBar) convertView.findViewById(R.id.sending_progress))
                                    .setVisibility(View.VISIBLE);
                        } else {
                            ((ProgressBar) convertView.findViewById(R.id.sending_progress))
                                    .setVisibility(View.GONE);
                        }
                    } else {
                        if (Objects.requireNonNull(getItem(position - 1)).type != item.type) {
                            Global.setAvatarShape(ctx, convertView.findViewById(R.id.companion_avatar));
                            ((ImageView) convertView.findViewById(R.id.companion_avatar)).setImageTintList(null);
                            GlideApp.with(ctx)
                                    .load(String.format("%s/photos_cache/conversations_avatars/avatar_%s",
                                            ctx.getCacheDir().getAbsolutePath(), item.id))
                                    .error(ctx.getResources().getDrawable(R.drawable.circular_avatar))
                                    .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                                    .dontAnimate().centerCrop()
                                    .into((ImageView) convertView.findViewById(R.id.companion_avatar));
                        } else {
                            ((ImageView) convertView.findViewById(R.id.companion_avatar))
                                    .setVisibility(View.INVISIBLE);
                        }
                    }
                    CardView cardView;
                    boolean isDarkTheme = PreferenceManager.getDefaultSharedPreferences(ctx)
                            .getBoolean("dark_theme", false);
                    if (item.type == 0) {
                        cardView = ((CardView) convertView.findViewById(R.id.incoming_msg_layout));
                        if (Global.checkMonet(ctx)) {
                            MonetCompat monet = MonetCompat.getInstance();
                            cardView.setCardBackgroundColor(
                                    Objects.requireNonNull(monet.getMonetColors().getAccent1().get(500))
                                            .toLinearSrgb().toSrgb().quantize8());
                        } else {
                            if (isDarkTheme) {
                                cardView.setCardBackgroundColor(
                                        MaterialColors.getColor(convertView, androidx.appcompat.R.attr.colorPrimaryDark));
                            } else {
                                cardView.setCardBackgroundColor(
                                        MaterialColors.getColor(convertView, androidx.appcompat.R.attr.colorAccent));
                            }
                        }
                    }
                } else {
                    msg_text.setTypeface(Global.getFlexibleTypeface(ctx, 500));
                    msg_text.setText(Global.formatLinksAsHtml(item.text));
                    msg_text.setMovementMethod(LinkMovementMethod.getInstance());
                }
            } catch (Exception | AssertionError ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        try {
            return ((Message) Objects.requireNonNull(getItem(position))).type;
        } catch (Exception ex) {
            return 0;
        }
    }

    private Message getItem(int position) {
        try {
            if (position >= 0) {
                return items.get(position);
            } else {
                return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }
}
