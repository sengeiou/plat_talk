package com.kylindev.totalk.app;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.kylindev.totalk.R;
import com.kylindev.totalk.net.Member;

import java.util.ArrayList;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ListHolder> {
    Context context;
    ArrayList<Member> members;
    String s_members="";

    public MemberAdapter(Context context, ArrayList<Member> members) {
        this.context = context;
        this.members = members;
    }

    public String getS_members() {
        return s_members;
    }

    public void setS_members(String s_members) {
        this.s_members = s_members;
    }

    @NonNull
    @Override
    public ListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member, parent, false);
        ListHolder holder=new ListHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ListHolder holder, final int position) {
        holder.tv_name.setText(members.get(position).getNick());
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!s_members.equals("")){
                    s_members+=",";
                }
                s_members+=members.get(position).getUid();
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class ListHolder extends RecyclerView.ViewHolder {
        TextView tv_name;
        CheckBox checkBox;
        ListHolder(View itemView) {
            super(itemView);
            tv_name=itemView.findViewById(R.id.tv_name);
            checkBox=itemView.findViewById(R.id.checkbox);
        }
    }

}

