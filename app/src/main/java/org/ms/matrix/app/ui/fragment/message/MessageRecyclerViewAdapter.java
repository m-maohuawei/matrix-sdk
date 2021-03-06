package org.ms.matrix.app.ui.fragment.message;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.ms.matrix.app.R;
import org.ms.matrix.app.db.event.Event;
import org.ms.matrix.app.ui.activity.chat.ChatActivity;

import java.util.List;

public class MessageRecyclerViewAdapter extends RecyclerView.Adapter<MessageRecyclerViewAdapter.ViewHolder> {

    private Context context;
    List<Event> events;

    public MessageRecyclerViewAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = View.inflate(context, R.layout.item_recycler_view_message_list_adapter, null);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textViewRoomName.setText(events.get(position).get_room_id() + "");


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("roomid", events.get(position).get_room_id());
                intent.putExtra("content", events.get(position).get_content());
                context.startActivity(intent);

            }
        });


    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {


        private ImageView imageViewAvatar;
        private TextView textViewRoomName;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewRoomName = itemView.findViewById(R.id.textViewRoomName);
        }
    }
}
