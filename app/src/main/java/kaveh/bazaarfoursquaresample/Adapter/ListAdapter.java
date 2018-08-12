package kaveh.bazaarfoursquaresample.Adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

import kaveh.bazaarfoursquaresample.MainActivity;
import kaveh.bazaarfoursquaresample.Model.Item;
import kaveh.bazaarfoursquaresample.R;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    private List<Item> venues;
    private MainActivity activity;

    public ListAdapter(List<Item> myDataset, MainActivity act) {
        venues = myDataset;
        this.activity = act;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView txtName;
        private TextView txtDistance;
        public View layout;
        private ImageView icon;

        private ViewHolder(View v) {
            super(v);
            layout = v;
            txtName = v.findViewById(R.id.name);
            txtDistance = v.findViewById(R.id.distance);
            icon = v.findViewById(R.id.icon);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            activity.recyclerViewListClicked(v, this.getLayoutPosition());
        }
    }

    @Override
    public ListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.list_item_layout, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.txtName.setText((position + 1) + " " + venues.get(position).getVenue().getName());
        holder.txtDistance.setText(String.valueOf(venues.get(position).getVenue().getLocation().getDistance()) + "m");
        try {
            final String imageUrl = venues.get(position).getVenue().getCategories().get(0).getIcon().getPrefix() +
                    "88" + venues.get(position).getVenue().getCategories().get(0).getIcon().getSuffix();
            Picasso.get()
                    .load(imageUrl)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(holder.icon, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            holder.icon.getDrawable().setColorFilter(Color.parseColor("#3F5AF4"), PorterDuff.Mode.MULTIPLY);
                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get()
                                    .load(imageUrl)
                                    .into(holder.icon, new com.squareup.picasso.Callback() {
                                        @Override
                                        public void onSuccess() {
                                            holder.icon.getDrawable().setColorFilter(Color.parseColor("#3F5AF4"), PorterDuff.Mode.MULTIPLY);
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                        }
                    });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return venues.size();
    }
}