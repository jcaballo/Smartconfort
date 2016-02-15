package com.alyj.smartconfort.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.alyj.smartconfort.R;
import com.alyj.smartconfort.model.Characteristiques;

import java.util.List;

/**
 * Created by yirou on 17/11/15.
 */
public class CharacteristicsAdapter extends BaseAdapter {
    Integer[] char_image;
    LayoutInflater inflater;
    private List<Characteristiques> characteristiques;
    private Context context;

    public CharacteristicsAdapter(Context context, List<Characteristiques> characteristiques) {
        this.context = context;
        this.characteristiques = characteristiques;
        inflater = LayoutInflater.from(this.context);
    }

    @Override
    public int getCount() {
        return characteristiques.size();
    }

    @Override
    public Object getItem(int position) {
        return characteristiques.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public List<Characteristiques> getFleetBeacons() {
        return characteristiques;
    }

    public void setFleetBeacons(List<Characteristiques> fleetBeacons) {
        this.characteristiques = fleetBeacons;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        MyViewHolder mViewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_view_maquette, parent, false);
            mViewHolder = new MyViewHolder(convertView);
            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (MyViewHolder) convertView.getTag();
        }

        Characteristiques characteristic = (Characteristiques) getItem(position);

        mViewHolder.tvName.setText(characteristic.getName() +" | "+characteristic.getValue());
        mViewHolder.chars.setText(characteristic.getCharacteristic());
        mViewHolder.ivIcon.setImageResource(R.drawable.abc_btn_check_material);

        return convertView;


    }

    private class MyViewHolder {
        TextView tvName, chars;
        ImageView ivIcon;

        public MyViewHolder(View item) {
            tvName = (TextView) item.findViewById(R.id.firstLine);
            chars = (TextView) item.findViewById(R.id.secondLine);
            ivIcon = (ImageView) item.findViewById(R.id.icon);
        }
    }
}
