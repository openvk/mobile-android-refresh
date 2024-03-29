package uk.openvk.android.refresh.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.preference.PreferenceManager;
import uk.openvk.android.refresh.OvkApplication;
import uk.openvk.android.refresh.api.OpenVKAPI;
import uk.openvk.android.refresh.api.enumerations.HandlerMessages;
import uk.openvk.android.refresh.api.wrappers.DownloadManager;
import uk.openvk.android.refresh.api.wrappers.OvkAPIWrapper;
import uk.openvk.android.refresh.ui.core.activities.AppActivity;
import uk.openvk.android.refresh.ui.core.activities.AuthActivity;
import uk.openvk.android.refresh.ui.core.activities.ConversationActivity;
import uk.openvk.android.refresh.ui.core.activities.FriendsIntentActivity;
import uk.openvk.android.refresh.ui.core.activities.GroupIntentActivity;
import uk.openvk.android.refresh.ui.core.activities.NewPostActivity;
import uk.openvk.android.refresh.ui.core.activities.ProfileIntentActivity;
import uk.openvk.android.refresh.ui.core.activities.QuickSearchActivity;
import uk.openvk.android.refresh.ui.core.activities.WallPostActivity;
import uk.openvk.android.refresh.ui.core.activities.base.NetworkActivity;

public class OvkAPIReceiver extends BroadcastReceiver {
    private Activity activity;
    public OvkAPIReceiver(Activity _activity) {
        activity = _activity;
    }

    public OvkAPIReceiver() {

    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(activity instanceof final NetworkActivity netActivity) {
            OpenVKAPI ovk_api = netActivity.ovk_api;
            final Bundle data = intent.getExtras();
            if(data.getString("address").equals(activity.getLocalClassName())) {
                final Message msg = parseJSONData(ovk_api.wrapper, netActivity.handler, data);
                ovk_api.wrapper.handler.post(() -> {
                    Log.d(OvkApplication.API_TAG,
                            String.format("Handling message %s in %s (%s)", msg.what, activity.getLocalClassName(),
                                    data.getString("address"))
                    );
                    netActivity.receiveState(msg.what, data);
                });
            }
        }
    }

    public Message parseJSONData(OvkAPIWrapper wrapper, Handler handler, Bundle data) {
        Message msg = new Message();
        String method = data.getString("method");
        String args = data.getString("args");
        String where = data.getString("where");
        msg.setData(data);
        SharedPreferences global_prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        DownloadManager downloadManager = new DownloadManager(activity,
                global_prefs.getBoolean("useHTTPS", false), handler);

        downloadManager.setInstance(((OvkApplication) activity
                .getApplicationContext()).getCurrentInstance());
        if (activity instanceof AuthActivity) {
            assert method != null;
            if ("Account.getProfileInfo".equals(method)) {
                msg.what = HandlerMessages.ACCOUNT_PROFILE_INFO;
            }
        } else if (activity instanceof AppActivity app_a) {
            downloadManager = app_a.ovk_api.dlman;
            assert method != null;
            switch (method) {
                case "Account.getProfileInfo":
                    app_a.ovk_api.account.parse(data.getString("response"), wrapper);
                    msg.what = HandlerMessages.ACCOUNT_PROFILE_INFO;
                    break;
                case "Account.getCounters":
                    app_a.ovk_api.account.parseCounters(data.getString("response"));
                    msg.what = HandlerMessages.ACCOUNT_COUNTERS;
                    break;
                case "Newsfeed.get":
                    if (where != null && where.equals("more_news")) {
                        msg.what = HandlerMessages.NEWSFEED_GET_MORE;
                        app_a.ovk_api.newsfeed.parse(app_a,
                                downloadManager, data.getString("response"),
                                global_prefs.getString("photos_quality", ""), false);
                    } else {
                        msg.what = HandlerMessages.NEWSFEED_GET;
                        app_a.ovk_api.newsfeed.parse(app_a,
                                downloadManager, data.getString("response"),
                                global_prefs.getString("photos_quality", ""), true);
                    }
                    break;
                case "Newsfeed.getGlobal":
                    if (where != null && where.equals("more_news")) {
                        msg.what = HandlerMessages.NEWSFEED_GET_MORE_GLOBAL;
                        app_a.ovk_api.newsfeed.parse(activity,
                                downloadManager, data.getString("response"),
                                global_prefs.getString("photos_quality", ""), false);
                    } else {
                        msg.what = HandlerMessages.NEWSFEED_GET_GLOBAL;
                        app_a.ovk_api.newsfeed.parse(activity,
                                downloadManager, data.getString("response"),
                                global_prefs.getString("photos_quality", ""), true);
                    }
                    break;
                case "Messages.getLongPollServer":
                    app_a.longPollServer = app_a.ovk_api.messages
                            .parseLongPollServer(data.getString("response"));
                    msg.what = HandlerMessages.MESSAGES_GET_LONGPOLL_SERVER;
                    break;
                case "Likes.add":
                    app_a.ovk_api.likes.parse(data.getString("response"));
                    msg.what = HandlerMessages.LIKES_ADD;
                    break;
                case "Likes.delete":
                    app_a.ovk_api.likes.parse(data.getString("response"));
                    msg.what = HandlerMessages.LIKES_DELETE;
                    break;
                case "Users.get":
                    app_a.ovk_api.users.parse(data.getString("response"));
                    msg.what = HandlerMessages.USERS_GET;
                    break;
                case "Friends.get":
                    if (args != null && args.contains("offset")) {
                        msg.what = HandlerMessages.FRIENDS_GET_MORE;
                        app_a.ovk_api.friends.parse(data.getString("response"),
                                downloadManager, true, false);
                    } else {
                        assert where != null;
                        if(where.equals("profile_counter")) {
                            msg.what = HandlerMessages.FRIENDS_GET_ALT;
                            app_a.ovk_api.friends.parse(data.getString("response"),
                                    downloadManager, false, true);
                        } else {
                            msg.what = HandlerMessages.FRIENDS_GET;
                            app_a.ovk_api.friends.parse(data.getString("response"),
                                    downloadManager, true, true);
                        }
                    }
                    break;
                case "Friends.getRequests":
                    msg.what = HandlerMessages.FRIENDS_REQUESTS;
                    app_a.ovk_api.friends.parseRequests(data.getString("response"),
                            downloadManager, true);
                    break;
                case "Photos.getAlbums":
                    msg.what = HandlerMessages.PHOTOS_GETALBUMS;
                    if (args != null && args.contains("offset")) {
                        app_a.ovk_api.photos.parseAlbums(data.getString("response"),
                                downloadManager, false);
                    } else {
                        assert where != null;
                        app_a.ovk_api.photos.parseAlbums(data.getString("response"),
                                downloadManager, true);
                    }
                    break;
                case "Wall.get":
                    if(where != null && where.equals("more_wall_posts")) {
                        app_a.ovk_api.wall.parse(activity, downloadManager,
                                global_prefs.getString("photos_quality", ""),
                                data.getString("response"), false, true);
                        msg.what = HandlerMessages.WALL_GET_MORE;
                    } else {
                        app_a.ovk_api.wall.parse(activity, downloadManager,
                                global_prefs.getString("photos_quality", ""),
                                data.getString("response"), true, true);
                        msg.what = HandlerMessages.WALL_GET;
                    }
                    break;
                case "Messages.getConversations":
                    app_a.conversations = app_a.ovk_api.messages.parseConversationsList(
                            data.getString("response"),
                            downloadManager);
                    msg.what = HandlerMessages.MESSAGES_CONVERSATIONS;
                    break;
                case "Groups.get":
                    if (args != null && args.contains("offset")) {
                        msg.what = HandlerMessages.GROUPS_GET_MORE;
                        app_a.old_friends_size = app_a.ovk_api.groups.getList().size();
                        app_a.ovk_api.groups.parse(data.getString("response"),
                                downloadManager,
                                global_prefs.getString("photos_quality", ""),
                                true, false);
                    } else {
                        msg.what = HandlerMessages.GROUPS_GET;
                        app_a.ovk_api.groups.parse(data.getString("response"),
                                downloadManager,
                                global_prefs.getString("photos_quality", ""),
                                true, true);
                    }
                    break;
                case "Ovk.version":
                    msg.what = HandlerMessages.OVK_VERSION;
                    app_a.ovk_api.ovk.parseVersion(data.getString("response"));
                    break;
                case "Ovk.aboutInstance":
                    msg.what = HandlerMessages.OVK_ABOUTINSTANCE;
                    app_a.ovk_api.ovk.parseAboutInstance(data.getString("response"));
                    break;
                case "Notes.get":
                    msg.what = HandlerMessages.NOTES_GET;
                    app_a.ovk_api.notes.parse(data.getString("response"));
                    break;
            }
        } else if (activity instanceof ConversationActivity conv_a) {
            downloadManager = conv_a.ovk_api.dlman;
            assert method != null;
            switch (method) {
                case "Messages.getHistory" -> {
                    conv_a.history = conv_a.conversation.parseHistory(activity, data.getString("response"));
                    msg.what = HandlerMessages.MESSAGES_GET_HISTORY;
                }
                case "Messages.send" -> msg.what = HandlerMessages.MESSAGES_SEND;
                case "Messages.delete" -> msg.what = HandlerMessages.MESSAGES_DELETE;
            }
        } else if (activity instanceof FriendsIntentActivity friends_a) {
            downloadManager = friends_a.ovk_api.dlman;
            assert method != null;
            switch (method) {
                case "Account.getProfileInfo":
                    friends_a.ovk_api.account.parse(data.getString("response"), wrapper);
                    msg.what = HandlerMessages.ACCOUNT_PROFILE_INFO;
                    break;
                case "Friends.get":
                    if (args != null && args.contains("offset")) {
                        msg.what = HandlerMessages.FRIENDS_GET_MORE;
                        friends_a.ovk_api.friends.parse(data.getString("response"),
                                downloadManager, true, false);
                    } else {
                        msg.what = HandlerMessages.FRIENDS_GET;
                        friends_a.ovk_api.friends.parse(data.getString("response"),
                                downloadManager, true, true);
                    }
                    break;
            }
        } else if (activity instanceof ProfileIntentActivity profile_a) {
            downloadManager = profile_a.ovk_api.dlman;
            if(method == null) {
                method = "";
            }
            switch (method) {
                default:
                    Log.e(OvkApplication.API_TAG, String.format("[%s / %s] Method not found", method,
                            msg.what));
                    break;
                case "Account.getProfileInfo":
                    try {
                        profile_a.ovk_api.account.parse(data.getString("response"), wrapper);
                        msg.what = HandlerMessages.ACCOUNT_PROFILE_INFO;
                    } catch (Exception ex) {
                        msg.what = HandlerMessages.INTERNAL_ERROR;
                    }
                    break;
                case "Account.getCounters":
                    profile_a.ovk_api.account.parseCounters(data.getString("response"));
                    msg.what = HandlerMessages.ACCOUNT_COUNTERS;
                    break;
                case "Friends.get":
                    assert where != null;
                    if(where.equals("profile_counter")) {
                        msg.what = HandlerMessages.FRIENDS_GET_ALT;
                        profile_a.ovk_api.friends.parse(data.getString("response"),
                                downloadManager, false, true);
                    }
                    break;
                case "Users.get":
                    profile_a.ovk_api.users.parse(data.getString("response"));
                    profile_a.user = profile_a.ovk_api.users.getList().get(0);
                    msg.what = HandlerMessages.USERS_GET;
                    break;
                case "Users.search":
                    profile_a.ovk_api.users.parseSearch(data.getString("response"),
                            null);
                    msg.what = HandlerMessages.USERS_SEARCH;
                    break;
                case "Wall.get":
                    if(where != null && where.equals("more_wall_posts")) {
                        profile_a.ovk_api.wall.parse(activity, downloadManager,
                                global_prefs.getString("photos_quality", ""),
                                data.getString("response"), false, true);
                        msg.what = HandlerMessages.WALL_GET_MORE;
                    } else {
                        profile_a.ovk_api.wall.parse(activity, downloadManager,
                                global_prefs.getString("photos_quality", ""),
                                data.getString("response"), true, true);
                        msg.what = HandlerMessages.WALL_GET;
                    }
                    break;
                case "Likes.add":
                    profile_a.ovk_api.likes.parse(data.getString("response"));
                    msg.what = HandlerMessages.LIKES_ADD;
                    break;
                case "Likes.delete":
                    profile_a.ovk_api.likes.parse(data.getString("response"));
                    msg.what = HandlerMessages.LIKES_DELETE;
                    break;
            }
        } else if(activity instanceof GroupIntentActivity group_a) {
            downloadManager = group_a.ovk_api.dlman;
            assert method != null;
            switch (method) {
                case "Account.getProfileInfo":
                    group_a.ovk_api.account.parse(data.getString("response"), wrapper);
                    msg.what = HandlerMessages.ACCOUNT_PROFILE_INFO;
                    break;
                case "Groups.get":
                    group_a.ovk_api.groups.parse(data.getString("response"));
                    msg.what = HandlerMessages.USERS_GET;
                    break;
                case "Groups.getById":
                    group_a.ovk_api.groups.parse(data.getString("response"));
                    msg.what = HandlerMessages.GROUPS_GET_BY_ID;
                    break;
                case "Groups.search":
                    group_a.ovk_api.groups.parseSearch(data.getString("response"),
                            null);
                    msg.what = HandlerMessages.GROUPS_SEARCH;
                    break;
                case "Wall.get":
                    if(where != null && where.equals("more_wall_posts")) {
                        group_a.ovk_api.wall.parse(activity, downloadManager,
                                global_prefs.getString("photos_quality", ""),
                                data.getString("response"), false, true);
                        msg.what = HandlerMessages.WALL_GET_MORE;
                    } else {
                        group_a.ovk_api.wall.parse(activity, downloadManager,
                                global_prefs.getString("photos_quality", ""),
                                data.getString("response"), true, true);
                        msg.what = HandlerMessages.WALL_GET;
                    }
                    break;
                case "Likes.add":
                    group_a.ovk_api.likes.parse(data.getString("response"));
                    msg.what = HandlerMessages.LIKES_ADD;
                    break;
                case "Likes.delete":
                    group_a.ovk_api.likes.parse(data.getString("response"));
                    msg.what = HandlerMessages.LIKES_DELETE;
                    break;
            }
        /* doesn't work yet
        } else if(activity instanceof NotesIntentActivity) {
            NotesIntentActivity notes_a = ((NotesIntentActivity) activity);
            assert method != null;
            if(method.equals("Notes.get")) {
                msg.what = HandlerMessages.NOTES_GET;
                notes_a.ovk_api.notes.parse(data.getString("response"));
            }
        */
        } else if(activity instanceof NewPostActivity newpost_a) {
            assert method != null;
            if(method.equals("Wall.post")) {
                msg.what = HandlerMessages.WALL_POST;
            } else if(method.startsWith("Photos.get") && method.endsWith("Server")) {
                newpost_a.ovk_api.photos.parseUploadServer(data.getString("response"), method);
                msg.what = HandlerMessages.PHOTOS_UPLOAD_SERVER;
            } else if(method.startsWith("Photos.save")) {
                msg.what = HandlerMessages.PHOTOS_SAVE;
                newpost_a.ovk_api.photos.parseOnePhoto(data.getString("response"));
            }
        } else if(activity instanceof QuickSearchActivity quick_search_a) {
            downloadManager = quick_search_a.ovk_api.dlman;
            assert method != null;
            switch (method) {
                case "Groups.search":
                    quick_search_a.ovk_api.groups.parseSearch(data.getString("response"),
                            quick_search_a.ovk_api.dlman);
                    msg.what = HandlerMessages.GROUPS_SEARCH;
                    break;
                case "Users.search":
                    quick_search_a.ovk_api.users.parseSearch(data.getString("response"),
                            quick_search_a.ovk_api.dlman);
                    msg.what = HandlerMessages.USERS_SEARCH;
                    break;
            }
        /* doesn't work yet
        } else if(activity instanceof GroupMembersActivity) {
            GroupMembersActivity group_members_a = ((GroupMembersActivity) activity);
            downloadManager = group_members_a.ovk_api.dlman;
            assert method != null;
            switch (method) {
                case "Groups.getMembers":
                    group_members_a.group.members = new ArrayList<>();
                    group_members_a.group.parseMembers(data.getString("response"),
                            downloadManager, true);
                    msg.what = HandlerMessages.GROUP_MEMBERS;
                    break;
            }
        */
        } else if(activity instanceof WallPostActivity wall_post_a) {
            downloadManager = wall_post_a.ovk_api.dlman;
            assert method != null;
            if ("Wall.getComments".equals(method)) {
                wall_post_a.comments = wall_post_a.ovk_api.wall.parseComments(activity, downloadManager,
                        global_prefs.getString("photos_quality", ""),
                        data.getString("response"));
                msg.what = HandlerMessages.WALL_ALL_COMMENTS;
            }
        /* doesn't work yet
        } else if(activity instanceof PhotoAlbumIntentActivity) {
            PhotoAlbumIntentActivity album_a = ((PhotoAlbumIntentActivity) activity);
            downloadManager = album_a.ovk_api.dlman;
            assert method != null;
            switch (method) {
                case "Photos.getAlbums":
                    msg.what = HandlerMessages.PHOTOS_GETALBUMS;
                    if (args != null && args.contains("offset")) {
                        album_a.ovk_api.photos.parseAlbums(data.getString("response"),
                                downloadManager, false);
                    } else {
                        assert where != null;
                        album_a.ovk_api.photos.parseAlbums(data.getString("response"),
                                downloadManager, true);
                    }
                    break;
                case "Photos.get":
                    msg.what = HandlerMessages.PHOTOS_GET;
                    album_a.ovk_api.photos.parse(
                            data.getString("response"),
                            new PhotoAlbum(Long.parseLong(album_a.ids[1]), Long.parseLong(album_a.ids[0])),
                            downloadManager
                    );
                    break;
            }
        */
        }
        return msg;
    }
}
